package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;

import java.util.List;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {
    boolean existsByEvent_IdAndRequester_Id(Long eventId, Long requesterId);

    List<ParticipationRequest> findAllByRequester_Id(Long requesterId);

    List<ParticipationRequest> findAllByEvent_Id(Long eventId);

    long countByEvent_IdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findAllByEvent_IdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT r.event.id, COUNT(r) FROM ParticipationRequest r WHERE r.event.id IN :eventIds AND r.status = :status GROUP BY r.event.id")
    List<Object[]> countByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds, @Param("status") RequestStatus status);
}
