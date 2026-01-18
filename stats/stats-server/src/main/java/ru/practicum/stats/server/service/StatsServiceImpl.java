package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.exception.ValidationException;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.model.EndpointHit;
import ru.practicum.stats.server.model.ViewStats;
import ru.practicum.stats.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto hitDto) {
        log.info("Сохранение информации о запросе: app={}, uri={}, ip={}",
                hitDto.getApp(), hitDto.getUri(), hitDto.getIp());

        EndpointHit hit = StatsMapper.toEndpointHit(hitDto);
        EndpointHit savedHit = statsRepository.save(hit);

        return StatsMapper.toEndpointHitDto(savedHit);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        log.info("Получение статистики: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        if (start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть после даты окончания");
        }

        List<ViewStats> stats;

        if (uris == null || uris.isEmpty()) {
            stats = unique ? statsRepository.findAllStatsUnique(start, end)
                    : statsRepository.findAllStats(start, end);
        } else {
            stats = unique ? statsRepository.findStatsByUrisUnique(start, end, uris)
                    : statsRepository.findStatsByUris(start, end, uris);
        }

        return stats.stream()
                .map(StatsMapper::toViewStatsDto)
                .collect(Collectors.toList());
    }
}
