package ru.practicum.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.rating.service.RatingService;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PutMapping("/like")
    public void addLike(@PathVariable Long userId, @PathVariable Long eventId) {
        ratingService.addLike(userId, eventId);
    }

    @PutMapping("/dislike")
    public void addDislike(@PathVariable Long userId, @PathVariable Long eventId) {
        ratingService.addDislike(userId, eventId);
    }

    @DeleteMapping("/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRating(@PathVariable Long userId, @PathVariable Long eventId) {
        ratingService.removeRating(userId, eventId);
    }
}
