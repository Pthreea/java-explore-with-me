package ru.practicum.ewm.util;

import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.priv.dto.ParticipationRequestDto;

public class RequestMapper {

    public static ParticipationRequestDto toParticipationRequestDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().name())
                .build();
    }
}
