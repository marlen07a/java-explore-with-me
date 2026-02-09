package ru.practicum.rating.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.rating.model.Rating;
import ru.practicum.rating.repository.RatingRepository;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;

    @Override
    public void addLike(Long userId, Long eventId) {
        addRating(userId, eventId, true);
    }

    @Override
    public void addDislike(Long userId, Long eventId) {
        addRating(userId, eventId, false);
    }

    @Override
    public void removeRating(Long userId, Long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        Rating rating = ratingRepository.findByEvent_IdAndUser_Id(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Rating not found"));
        ratingRepository.delete(rating);
    }

    private void addRating(Long userId, Long eventId, boolean isLike) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Can only rate published events");
        }

        if (Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Event initiator cannot rate their own event");
        }

        boolean hasConfirmedRequest = requestRepository
                .existsByEvent_IdAndRequester_IdAndStatus(eventId, userId, RequestStatus.CONFIRMED);
        if (!hasConfirmedRequest) {
            throw new ConflictException("Only confirmed participants can rate events");
        }

        Optional<Rating> existing = ratingRepository.findByEvent_IdAndUser_Id(eventId, userId);
        if (existing.isPresent()) {
            Rating rating = existing.get();
            rating.setPositive(isLike);
            ratingRepository.save(rating);
        } else {
            Rating rating = new Rating();
            rating.setEvent(event);
            rating.setUser(user);
            rating.setPositive(isLike);
            ratingRepository.save(rating);
        }
    }
}
