package ru.practicum.ewm.admin.service;

import ru.practicum.ewm.admin.dto.NewCompilationDto;
import ru.practicum.ewm.admin.dto.UpdateCompilationRequest;
import ru.practicum.ewm.pub.dto.CompilationDto;

public interface AdminCompilationService {
    CompilationDto createCompilation(NewCompilationDto dto);

    void deleteCompilation(Long compId);

    CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request);
}
