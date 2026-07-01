package ru.practicum.ewm.admin.service;

import ru.practicum.ewm.admin.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.pub.dto.EventFullDto;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminEventService {
    List<EventFullDto> searchEvents(List<Long> users,
                                    List<String> states,
                                    List<Long> categories,
                                    LocalDateTime rangeStart,
                                    LocalDateTime rangeEnd,
                                    int from,
                                    int size);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request);
}
