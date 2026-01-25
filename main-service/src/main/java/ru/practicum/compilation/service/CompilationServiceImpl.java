package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;

    @Override
    public CompilationDto create(NewCompilationDto dto) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(Boolean.TRUE.equals(dto.getPinned()));
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllById(dto.getEvents());
            compilation.setEvents(new HashSet<>(events));
        }
        Compilation saved = compilationRepository.save(compilation);
        return CompilationMapper.toDto(saved, confirmedCounts(saved.getEvents()), Map.of());
    }

    @Override
    public void delete(Long id) {
        if (!compilationRepository.existsById(id)) {
            throw new NotFoundException("Compilation with id=" + id + " was not found");
        }
        compilationRepository.deleteById(id);
    }

    @Override
    public CompilationDto update(Long id, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + id + " was not found"));
        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
        if (dto.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(dto.getEvents());
            compilation.setEvents(new HashSet<>(events));
        }
        Compilation saved = compilationRepository.save(compilation);
        return CompilationMapper.toDto(saved, confirmedCounts(saved.getEvents()), Map.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> findAll(Boolean pinned, Pageable pageable) {
        List<Compilation> comps = pinned == null
                ? compilationRepository.findAll(pageable).getContent()
                : compilationRepository.findAllByPinned(pinned, pageable);
        Map<Long, Long> views = Map.of();
        Map<Long, Long> confirmed = confirmedCounts(comps.stream()
                .flatMap(c -> c.getEvents().stream())
                .collect(Collectors.toSet()));
        return comps.stream()
                .map(c -> CompilationMapper.toDto(c, confirmed, views))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getById(Long id) {
        Compilation comp = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + id + " was not found"));
        return CompilationMapper.toDto(comp, confirmedCounts(comp.getEvents()), Map.of());
    }

    private Map<Long, Long> confirmedCounts(Set<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = events.stream().map(Event::getId).collect(Collectors.toSet());
        return requestRepository.findAll().stream()
                .filter(r -> ids.contains(r.getEvent().getId()) && r.getStatus() == RequestStatus.CONFIRMED)
                .collect(Collectors.groupingBy(r -> r.getEvent().getId(), Collectors.counting()));
    }
}
