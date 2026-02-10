package ru.practicum.rating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.rating.model.Rating;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByEvent_IdAndUser_Id(Long eventId, Long userId);

    void deleteByEvent_IdAndUser_Id(Long eventId, Long userId);

    long countByEvent_IdAndPositive(Long eventId, Boolean positive);

    @Query("SELECT r.event.id, SUM(CASE WHEN r.positive = true THEN 1L ELSE -1L END) " +
           "FROM Rating r WHERE r.event.id IN :eventIds GROUP BY r.event.id")
    List<Object[]> calculateRatingsByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT r.event.initiator.id, SUM(CASE WHEN r.positive = true THEN 1L ELSE -1L END) " +
           "FROM Rating r WHERE r.event.initiator.id IN :userIds GROUP BY r.event.initiator.id")
    List<Object[]> calculateRatingsByUserIds(@Param("userIds") List<Long> userIds);
}
