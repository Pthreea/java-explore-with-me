package ru.practicum.ewm.priv.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.priv.dto.NewEventDto;
import ru.practicum.ewm.priv.dto.UpdateEventUserRequest;
import ru.practicum.ewm.pub.dto.EventFullDto;
import ru.practicum.ewm.pub.dto.EventShortDto;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.util.EventMapper;
import ru.practicum.ewm.util.LocationMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrivateEventServiceImpl implements PrivateEventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        log.info("Getting events for user {}: from={}, size={}", userId, from, size);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();

        log.info("Found {} events for user {}", events.size(), userId);

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = requestRepository.countByEventIdAndStatus(
                            event.getId(), RequestStatus.CONFIRMED);
                    return EventMapper.toEventShortDto(event, confirmedRequests, 0L);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        log.info("User {} creating event: {}", userId, dto.getTitle());

        LocalDateTime minEventDate = LocalDateTime.now().plusHours(2);
        if (dto.getEventDate().isBefore(minEventDate)) {
            throw new ValidationException("Event date must be at least 2 hours from now");
        }

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found"));

        Event event = EventMapper.toEvent(dto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);

        log.info("Event created with id: {}", savedEvent.getId());
        return EventMapper.toEventFullDto(savedEvent, 0L, 0L);
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        log.info("Getting event {} for user {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(
                event.getId(), RequestStatus.CONFIRMED);

        return EventMapper.toEventFullDto(event, confirmedRequests, 0L);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("User {} updating event {}", userId, eventId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (request.getEventDate() != null) {
            LocalDateTime minEventDate = LocalDateTime.now().plusHours(2);
            if (request.getEventDate().isBefore(minEventDate)) {
                throw new ValidationException("Event date must be at least 2 hours from now");
            }
        }

        if (request.getStateAction() != null) {
            UserStateAction action = UserStateAction.valueOf(request.getStateAction());

            switch (action) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;

                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        updateEventFields(event, request);

        Event updatedEvent = eventRepository.save(event);
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(
                updatedEvent.getId(), RequestStatus.CONFIRMED);

        log.info("Event {} updated", eventId);
        return EventMapper.toEventFullDto(updatedEvent, confirmedRequests, 0L);
    }

    private void updateEventFields(Event event, UpdateEventUserRequest request) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) {
            event.setLocation(LocationMapper.toLocation(request.getLocation()));
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }
}
