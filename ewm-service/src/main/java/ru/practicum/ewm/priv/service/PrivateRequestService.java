package ru.practicum.ewm.priv.service;

import ru.practicum.ewm.priv.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.priv.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.priv.dto.ParticipationRequestDto;

import java.util.List;

public interface PrivateRequestService {
    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId,
                                                        EventRequestStatusUpdateRequest request);
}
