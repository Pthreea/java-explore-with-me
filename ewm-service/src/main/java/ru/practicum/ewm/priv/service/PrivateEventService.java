package ru.practicum.ewm.priv.service;

import ru.practicum.ewm.priv.dto.NewEventDto;
import ru.practicum.ewm.priv.dto.UpdateEventUserRequest;
import ru.practicum.ewm.pub.dto.EventFullDto;
import ru.practicum.ewm.pub.dto.EventShortDto;

import java.util.List;

public interface PrivateEventService {
    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventFullDto createEvent(Long userId, NewEventDto dto);

    EventFullDto getUserEventById(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request);
}
