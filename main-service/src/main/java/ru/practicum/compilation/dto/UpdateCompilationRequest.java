package ru.practicum.compilation.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;

import java.util.List;

@Data
public class UpdateCompilationRequest {
    private List<Long> events;
    private Boolean pinned;
    @Size(max = 50)
    private String title;
}
