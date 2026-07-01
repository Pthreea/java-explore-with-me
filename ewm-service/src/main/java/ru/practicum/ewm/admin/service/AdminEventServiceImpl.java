package ru.practicum.ewm.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.admin.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.pub.dto.EventFullDto;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.util.EventMapper;
import ru.practicum.ewm.util.LocationMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEventServiceImpl implements AdminEventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;

    @Override
    public List<EventFullDto> searchEvents(List<Long> users,
                                           List<String> states,
                                           List<Long> categories,
                                           LocalDateTime rangeStart,
                                           LocalDateTime rangeEnd,
                                           int from,
                                           int size) {
        log.info("Admin searching events: users={}, states={}, categories={}", users, states, categories);

        List<EventState> eventStates = states != null ?
                states.stream().map(EventState::valueOf).collect(Collectors.toList()) : null;

        Pageable pageable = PageRequest.of(from / size, size);

        List<Event> events = eventRepository.findAdminEvents(
                users, eventStates, categories, rangeStart, rangeEnd, pageable
        ).getContent();

        log.info("Found {} events", events.size());

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = requestRepository.countByEventIdAndStatus(
                            event.getId(), RequestStatus.CONFIRMED);
                    return EventMapper.toEventFullDto(event, confirmedRequests, 0L);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Admin updating event {}: {}", eventId, request);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (request.getEventDate() != null) {
            LocalDateTime minPublishDate = LocalDateTime.now().plusHours(1);
            if (request.getEventDate().isBefore(minPublishDate)) {
                throw new ValidationException("Event date must be at least 1 hour from now");
            }
        }

        if (request.getStateAction() != null) {
            AdminStateAction action = AdminStateAction.valueOf(request.getStateAction());

            switch (action) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Cannot publish the event because it's not in the right state: "
                                + event.getState());
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject the event because it's already published");
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        updateEventFields(event, request);

        Event updatedEvent = eventRepository.save(event);
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(
                updatedEvent.getId(), RequestStatus.CONFIRMED);

        log.info("Event {} updated by admin", eventId);
        return EventMapper.toEventFullDto(updatedEvent, confirmedRequests, 0L);
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest request) {
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
