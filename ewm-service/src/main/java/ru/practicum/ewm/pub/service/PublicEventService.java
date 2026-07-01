package ru.practicum.ewm.pub.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.pub.dto.EventFullDto;
import ru.practicum.ewm.pub.dto.EventShortDto;

import java.time.LocalDateTime;
import java.util.List;

public interface PublicEventService {
    List<EventShortDto> searchEvents(String text,
                                     List<Long> categories,
                                     Boolean paid,
                                     LocalDateTime rangeStart,
                                     LocalDateTime rangeEnd,
                                     Boolean onlyAvailable,
                                     String sort,
                                     int from,
                                     int size,
                                     HttpServletRequest request);

    EventFullDto getEventById(Long id, HttpServletRequest request);
}
