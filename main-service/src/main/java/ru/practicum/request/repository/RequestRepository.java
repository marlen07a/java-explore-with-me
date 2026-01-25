package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {
    boolean existsByEvent_IdAndRequester_Id(Long eventId, Long requesterId);

    List<ParticipationRequest> findAllByRequester_Id(Long requesterId);

    List<ParticipationRequest> findAllByEvent_Id(Long eventId);

    long countByEvent_IdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findAllByEvent_IdAndStatus(Long eventId, RequestStatus status);

    default Map<Long, Long> countConfirmed(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        return findAll().stream()
                .filter(r -> eventIds.contains(r.getEvent().getId()) && r.getStatus() == RequestStatus.CONFIRMED)
                .collect(Collectors.groupingBy(r -> r.getEvent().getId(), Collectors.counting()));
    }
}
