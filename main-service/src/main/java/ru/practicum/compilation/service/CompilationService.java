package ru.practicum.compilation.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto dto);

    void delete(Long id);

    CompilationDto update(Long id, UpdateCompilationRequest dto);

    List<CompilationDto> findAll(Boolean pinned, Pageable pageable);

    CompilationDto getById(Long id);
}
