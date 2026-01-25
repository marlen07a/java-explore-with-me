package ru.practicum.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    List<Event> findAllByInitiator_Id(Long initiatorId, org.springframework.data.domain.Pageable pageable);

    boolean existsByCategory_Id(Long categoryId);

    @Query("SELECT e FROM Event e WHERE e.state = :state AND e.id IN :ids")
    List<Event> findAllByIdInAndState(@Param("ids") List<Long> ids, @Param("state") EventState state);

    @Query("SELECT e FROM Event e WHERE e.state = 'PUBLISHED' AND e.id = :id")
    Event findPublishedById(@Param("id") Long id);

    @Query("SELECT e FROM Event e WHERE e.state = 'PUBLISHED' AND (e.eventDate >= :start) AND (e.eventDate <= :end)")
    List<Event> findPublishedBetween(LocalDateTime start, LocalDateTime end);
}
