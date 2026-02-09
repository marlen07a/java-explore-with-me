package ru.practicum.event.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.Location;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;

@UtilityClass
public class EventMapper {

    public static Event toEntity(NewEventDto dto, Category category, User initiator, LocalDateTime createdOn) {
        Event event = new Event();
        event.setAnnotation(dto.getAnnotation());
        event.setCategory(category);
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setLocation(toLocation(dto.getLocation()));
        event.setPaid(Boolean.TRUE.equals(dto.getPaid()));
        event.setParticipantLimit(dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit());
        event.setRequestModeration(dto.getRequestModeration() == null || dto.getRequestModeration());
        event.setTitle(dto.getTitle());
        event.setCreatedOn(createdOn);
        event.setInitiator(initiator);
        event.setState(EventState.PENDING);
        return event;
    }

    public static EventShortDto toShort(Event event, long confirmed, long views, long rating) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(confirmed)
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toShort(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .rating(rating)
                .build();
    }

    public static EventShortDto toShort(Event event, long confirmed, long views) {
        return toShort(event, confirmed, views, 0);
    }

    public static EventFullDto toFull(Event event, long confirmed, long views, long rating) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(confirmed)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toShort(event.getInitiator()))
                .location(toLocationDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .publishedOn(event.getPublishedOn())
                .state(event.getState().name())
                .title(event.getTitle())
                .views(views)
                .rating(rating)
                .build();
    }

    public static EventFullDto toFull(Event event, long confirmed, long views) {
        return toFull(event, confirmed, views, 0);
    }

    public static Location toLocation(LocationDto dto) {
        if (dto == null) {
            return null;
        }
        Location location = new Location();
        location.setLat(dto.getLat());
        location.setLon(dto.getLon());
        return location;
    }

    public static LocationDto toLocationDto(Location location) {
        if (location == null) {
            return null;
        }
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}
