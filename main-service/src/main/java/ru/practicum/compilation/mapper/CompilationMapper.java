package ru.practicum.compilation.mapper;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.event.mapper.EventMapper;
import java.util.Map;
import java.util.stream.Collectors;

public final class CompilationMapper {
    private CompilationMapper() {
    }

    public static CompilationDto toDto(Compilation compilation,
                                       Map<Long, Long> confirmed,
                                       Map<Long, Long> views) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(compilation.getEvents().stream()
                        .map(event -> EventMapper.toShort(
                                event,
                                confirmed.getOrDefault(event.getId(), 0L),
                                views.getOrDefault(event.getId(), 0L)))
                        .collect(Collectors.toList()))
                .build();
    }
}
