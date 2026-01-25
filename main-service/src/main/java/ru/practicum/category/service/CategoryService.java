package ru.practicum.category.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto dto);

    void delete(Long id);

    CategoryDto update(Long id, NewCategoryDto dto);

    List<CategoryDto> getAll(Pageable pageable);

    CategoryDto getById(Long id);
}
