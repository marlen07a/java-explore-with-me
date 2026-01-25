package ru.practicum.event.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.dto.UpdateEventUserRequest;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.event.model.StateActionAdmin;
import ru.practicum.event.model.StateActionUser;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    @Value("${spring.application.name:main-service}")
    private String appName;

    private static final String EVENT_BASE_PATH = "/events/";

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found"));

        LocalDateTime now = LocalDateTime.now();
        if (dto.getEventDate().isBefore(now.plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours in future");
        }

        Event event = new Event();
        event.setAnnotation(dto.getAnnotation());
        event.setCategory(category);
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setLocation(EventMapper.toLocation(dto.getLocation()));
        event.setPaid(Boolean.TRUE.equals(dto.getPaid()));
        event.setParticipantLimit(dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit());
        event.setRequestModeration(dto.getRequestModeration() == null || dto.getRequestModeration());
        event.setTitle(dto.getTitle());
        event.setCreatedOn(now);
        event.setInitiator(initiator);
        event.setState(EventState.PENDING);

        Event saved = eventRepository.save(event);
        return EventMapper.toFull(saved, 0, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        ensureUserExists(userId);
        List<Event> events = eventRepository.findAllByInitiator_Id(userId, pageable);
        Map<Long, Long> confirmed = confirmedCounts(events);
        Map<Long, Long> views = viewCounts(events);
        return events.stream()
                .map(ev -> EventMapper.toShort(ev,
                        confirmed.getOrDefault(ev.getId(), 0L),
                        views.getOrDefault(ev.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = getOwnedEvent(userId, eventId);
        long confirmed = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);
        long views = viewCounts(List.of(event)).getOrDefault(event.getId(), 0L);
        return EventMapper.toFull(event, confirmed, views);
    }

    @Override
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = getOwnedEvent(userId, eventId);
        if (!(event.getState() == EventState.PENDING || event.getState() == EventState.CANCELED)) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        applyCommonUpdates(event, dto.getAnnotation(), dto.getDescription(), dto.getCategory(), dto.getEventDate(),
                dto.getLocation(), dto.getPaid(), dto.getParticipantLimit(), dto.getRequestModeration(), dto.getTitle());

        if (dto.getStateAction() != null) {
            StateActionUser action = StateActionUser.valueOf(dto.getStateAction());
            if (action == StateActionUser.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            } else if (action == StateActionUser.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            }
        }
        Event saved = eventRepository.save(event);
        long confirmed = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);
        long views = viewCounts(List.of(saved)).getOrDefault(saved.getId(), 0L);
        return EventMapper.toFull(saved, confirmed, views);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getRequests(Long userId, Long eventId) {
        getOwnedEvent(userId, eventId);
        return requestRepository.findAllByEvent_Id(eventId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        Event event = getOwnedEvent(userId, eventId);
        List<ParticipationRequest> requests = requestRepository.findAllById(dto.getRequestIds());
        if (requests.stream().anyMatch(r -> r.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Request must have status PENDING");
        }

        RequestStatus newStatus = RequestStatus.valueOf(dto.getStatus());
        long limit = event.getParticipantLimit();
        long confirmedNow = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if (newStatus == RequestStatus.CONFIRMED) {
            for (ParticipationRequest r : requests) {
                if (limit != 0 && confirmedNow >= limit) {
                    r.setStatus(RequestStatus.REJECTED);
                    rejected.add(r);
                    continue;
                }
                r.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(r);
                confirmedNow++;
            }
        } else {
            requests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
            rejected.addAll(requests);
        }
        requestRepository.saveAll(requests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed.stream().map(RequestMapper::toDto).collect(Collectors.toList()))
                .rejectedRequests(rejected.stream().map(RequestMapper::toDto).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> adminSearch(List<Long> users, List<String> states, List<Long> categories,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd, Pageable pageable) {
        Specification<Event> spec = buildAdminSpec(users, states, categories, rangeStart, rangeEnd);
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        Map<Long, Long> confirmed = confirmedCounts(events);
        Map<Long, Long> views = viewCounts(events);
        return events.stream()
                .map(ev -> EventMapper.toFull(ev,
                        confirmed.getOrDefault(ev.getId(), 0L),
                        views.getOrDefault(ev.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto adminUpdate(Long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        applyCommonUpdates(event, dto.getAnnotation(), dto.getDescription(), dto.getCategory(), dto.getEventDate(),
                dto.getLocation(), dto.getPaid(), dto.getParticipantLimit(), dto.getRequestModeration(), dto.getTitle());

        if (dto.getStateAction() != null) {
            StateActionAdmin action = StateActionAdmin.valueOf(dto.getStateAction());
            if (action == StateActionAdmin.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Event date too soon for publication");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (action == StateActionAdmin.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject published event");
                }
                event.setState(EventState.CANCELED);
            }
        }

        Event saved = eventRepository.save(event);
        long confirmed = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);
        long views = viewCounts(List.of(saved)).getOrDefault(saved.getId(), 0L);
        return EventMapper.toFull(saved, confirmed, views);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> publicSearch(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            Boolean onlyAvailable, String sort, Pageable pageable, String ip, String uri) {
        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();
        if (rangeEnd != null && start.isAfter(rangeEnd)) {
            throw new BadRequestException("Start must be before end");
        }
        Specification<Event> spec = buildPublicSpec(text, categories, paid, start, rangeEnd);
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        Map<Long, Long> confirmed = confirmedCounts(events);
        Map<Long, Long> views = viewCounts(events);

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream()
                    .filter(ev -> ev.getParticipantLimit() == 0
                            || confirmed.getOrDefault(ev.getId(), 0L) < ev.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        if ("VIEWS".equalsIgnoreCase(sort)) {
            events.sort(Comparator.comparingLong(ev -> views.getOrDefault(ev.getId(), 0L)));
        } else {
            events.sort(Comparator.comparing(Event::getEventDate));
        }

        statsClient.saveHit(appName, uri, ip, LocalDateTime.now());

        return events.stream()
                .map(ev -> EventMapper.toShort(ev,
                        confirmed.getOrDefault(ev.getId(), 0L),
                        views.getOrDefault(ev.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublishedEvent(Long id, String ip, String uri) {
        Event event = eventRepository.findPublishedById(id);
        if (event == null) {
            throw new NotFoundException("Event with id=" + id + " was not found");
        }
        statsClient.saveHit(appName, uri, ip, LocalDateTime.now());
        long confirmed = requestRepository.countByEvent_IdAndStatus(id, RequestStatus.CONFIRMED);
        long views = viewCounts(List.of(event)).getOrDefault(event.getId(), 0L);
        return EventMapper.toFull(event, confirmed, views);
    }

    private void applyCommonUpdates(Event event, String annotation, String description, Long categoryId,
                                    LocalDateTime eventDate, ru.practicum.event.dto.LocationDto locationDto,
                                    Boolean paid, Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null) {
            event.setAnnotation(annotation);
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category with id=" + categoryId + " was not found"));
            event.setCategory(category);
        }
        if (eventDate != null) {
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ConflictException("Event date must be at least 2 hours in future");
            }
            event.setEventDate(eventDate);
        }
        if (locationDto != null) {
            Location location = EventMapper.toLocation(locationDto);
            event.setLocation(location);
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (title != null) {
            event.setTitle(title);
        }
    }

    private Event getOwnedEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return event;
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private Specification<Event> buildAdminSpec(List<Long> users, List<String> states, List<Long> categories,
                                                LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (users != null && !users.isEmpty()) {
                predicates.add(root.get("initiator").get("id").in(users));
            }
            if (states != null && !states.isEmpty()) {
                List<EventState> stateEnums = states.stream()
                        .map(s -> EventState.valueOf(s.toUpperCase(Locale.ROOT)))
                        .toList();
                predicates.add(root.get("state").in(stateEnums));
            }
            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }
            if (rangeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Event> buildPublicSpec(String text, List<Long> categories, Boolean paid,
                                                 LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));
            if (text != null && !text.isBlank()) {
                String pattern = "%" + text.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("annotation")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }
            if (paid != null) {
                predicates.add(cb.equal(root.get("paid"), paid));
            }
            if (rangeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Map<Long, Long> viewCounts(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<String> uris = events.stream()
                .map(ev -> EVENT_BASE_PATH + ev.getId())
                .toList();
        List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);
        return stats.stream()
                .collect(Collectors.toMap(
                        v -> extractEventId(v.getUri()),
                        ViewStatsDto::getHits,
                        (a, b) -> a));
    }

    private Map<Long, Long> confirmedCounts(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = events.stream().map(Event::getId).collect(Collectors.toSet());
        return requestRepository.findAll().stream()
                .filter(r -> ids.contains(r.getEvent().getId()) && r.getStatus() == RequestStatus.CONFIRMED)
                .collect(Collectors.groupingBy(r -> r.getEvent().getId(), Collectors.counting()));
    }

    private long extractEventId(String uri) {
        if (uri == null || !uri.contains(EVENT_BASE_PATH)) {
            return 0L;
        }
        String idPart = uri.substring(uri.lastIndexOf('/') + 1);
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
