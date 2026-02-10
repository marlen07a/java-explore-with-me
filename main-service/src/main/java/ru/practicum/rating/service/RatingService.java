package ru.practicum.rating.service;

public interface RatingService {
    void addLike(Long userId, Long eventId);

    void addDislike(Long userId, Long eventId);

    void removeRating(Long userId, Long eventId);
}
