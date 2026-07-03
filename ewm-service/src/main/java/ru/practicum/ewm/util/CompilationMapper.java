package ru.practicum.ewm.util;

import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.pub.dto.CompilationDto;
import ru.practicum.ewm.pub.dto.EventShortDto;
import ru.practicum.ewm.repository.RequestRepository;

import java.util.List;
import java.util.stream.Collectors;

public class CompilationMapper {

    public static CompilationDto toCompilationDto(Compilation compilation,
                                                  RequestRepository requestRepository) {
        List<EventShortDto> eventShortDtos = compilation.getEvents().stream()
                .map(event -> {
                    Long confirmedRequests = requestRepository.countByEventIdAndStatus(
                            event.getId(), RequestStatus.CONFIRMED);
                    return EventMapper.toEventShortDto(event, confirmedRequests, 0L);
                })
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .events(eventShortDtos)
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }
}
