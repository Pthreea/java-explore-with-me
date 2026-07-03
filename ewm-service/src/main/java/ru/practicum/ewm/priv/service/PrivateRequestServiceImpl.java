package ru.practicum.ewm.priv.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.priv.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.priv.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.priv.dto.ParticipationRequestDto;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.util.RequestMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrivateRequestServiceImpl implements PrivateRequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Getting requests for user {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);

        log.info("Found {} requests for user {}", requests.size(), userId);
        return requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("User {} creating request for event {}", userId, eventId);

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Event initiator cannot create participation request");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists");
        }

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(requester)
                .created(LocalDateTime.now())
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);

        log.info("Request created with id: {}, status: {}", savedRequest.getId(), savedRequest.getStatus());
        return RequestMapper.toParticipationRequestDto(savedRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("User {} canceling request {}", userId, requestId);

        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        log.info("Request {} canceled", requestId);
        return RequestMapper.toParticipationRequestDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Getting requests for event {} by user {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        log.info("Found {} requests for event {}", requests.size(), eventId);
        return requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId,
                                                               EventRequestStatusUpdateRequest updateRequest) {
        log.info("User {} updating requests for event {}: {}", userId, eventId, updateRequest);

        Event event = validateEventOwnership(userId, eventId);
        validateParticipantLimit(event);

        List<ParticipationRequest> requests = getAndValidateRequests(updateRequest.getRequestIds());

        RequestStatus newStatus = RequestStatus.valueOf(updateRequest.getStatus());
        int availableSlots = calculateAvailableSlots(event);

        EventRequestStatusUpdateResult result = processRequests(requests, newStatus, availableSlots);

        if (availableSlots == 0 && event.getParticipantLimit() > 0) {
            rejectRemainingPendingRequests(eventId, updateRequest.getRequestIds(), result);
        }

        log.info("Updated {} requests: {} confirmed, {} rejected",
                requests.size(), result.getConfirmedRequests().size(), result.getRejectedRequests().size());

        return result;
    }

    private Event validateEventOwnership(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private void validateParticipantLimit(Event event) {
        Long confirmedCount = requestRepository.countByEventIdAndStatus(
                event.getId(), RequestStatus.CONFIRMED);

        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit already reached");
        }
    }

    private List<ParticipationRequest> getAndValidateRequests(List<Long> requestIds) {
        List<ParticipationRequest> requests = requestRepository.findAllById(requestIds);

        boolean hasNonPending = requests.stream()
                .anyMatch(r -> r.getStatus() != RequestStatus.PENDING);

        if (hasNonPending) {
            throw new ConflictException("Can only update requests with status PENDING");
        }

        return requests;
    }

    private int calculateAvailableSlots(Event event) {
        if (event.getParticipantLimit() == 0) {
            return Integer.MAX_VALUE;
        }

        Long confirmedCount = requestRepository.countByEventIdAndStatus(
                event.getId(), RequestStatus.CONFIRMED);

        return (int) (event.getParticipantLimit() - confirmedCount);
    }

    private EventRequestStatusUpdateResult processRequests(List<ParticipationRequest> requests,
                                                           RequestStatus newStatus,
                                                           int availableSlots) {
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        int slots = availableSlots;

        for (ParticipationRequest request : requests) {
            if (newStatus == RequestStatus.CONFIRMED && slots > 0) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(RequestMapper.toParticipationRequestDto(request));
                slots--;
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toParticipationRequestDto(request));
            }
            requestRepository.save(request);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    private void rejectRemainingPendingRequests(Long eventId, List<Long> processedIds,
                                                EventRequestStatusUpdateResult result) {
        List<ParticipationRequest> pendingRequests = requestRepository.findPendingRequestsByEventId(eventId);

        for (ParticipationRequest pendingRequest : pendingRequests) {
            if (!processedIds.contains(pendingRequest.getId())) {
                pendingRequest.setStatus(RequestStatus.REJECTED);
                requestRepository.save(pendingRequest);
                result.getRejectedRequests().add(RequestMapper.toParticipationRequestDto(pendingRequest));
            }
        }
    }
}
