package ru.practicum.ewm.pub.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.pub.dto.EventFullDto;
import ru.practicum.ewm.pub.dto.EventShortDto;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.util.EventMapper;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicEventServiceImpl implements PublicEventService {

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public List<EventShortDto> searchEvents(String text,
                                            List<Long> categories,
                                            Boolean paid,
                                            LocalDateTime rangeStart,
                                            LocalDateTime rangeEnd,
                                            Boolean onlyAvailable,
                                            String sort,
                                            int from,
                                            int size,
                                            HttpServletRequest request) {

        log.info("Public search events: text={}, categories={}, paid={}, from={}, size={}",
                text, categories, paid, from, size);

        try {
            saveStats(request);

            if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
                throw new ValidationException("Start date must be before end date");
            }

            LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();
            Pageable pageable = createPageable(from, size, sort);

            List<Long> cats = (categories != null && !categories.isEmpty()) ? categories : null;

            log.info("Executing query with: text={}, categories={}, paid={}, start={}, end={}",
                    text, cats, paid, start, rangeEnd);

            List<Event> events = eventRepository.findPublicEvents(
                    text, cats, paid, start, rangeEnd, pageable
            ).getContent();

            log.info("Found {} events", events.size());

            if (Boolean.TRUE.equals(onlyAvailable)) {
                events = filterAvailableEvents(events);
            }

            Map<Long, Long> viewsMap = getViewsMap(events);

            List<EventShortDto> result = events.stream()
                    .map(event -> {
                        Long confirmedRequests = requestRepository.countByEventIdAndStatus(
                                event.getId(), RequestStatus.CONFIRMED);
                        Long views = viewsMap.getOrDefault(event.getId(), 0L);
                        return EventMapper.toEventShortDto(event, confirmedRequests, views);
                    })
                    .collect(Collectors.toList());

            if ("VIEWS".equalsIgnoreCase(sort)) {
                result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
            }

            return result;

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching events", e);
            throw new RuntimeException("Failed to search events: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public EventFullDto getEventById(Long id, HttpServletRequest request) {
        log.info("Getting public event by id: {}", id);

        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        saveStats(request);

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(id, RequestStatus.CONFIRMED);
        Long views = getViews(event);

        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    private Pageable createPageable(int from, int size, String sort) {
        if ("EVENT_DATE".equalsIgnoreCase(sort)) {
            return PageRequest.of(from / size, size, Sort.by("eventDate").ascending());
        }
        return PageRequest.of(from / size, size);
    }

    private List<Event> filterAvailableEvents(List<Event> events) {
        return events.stream()
                .filter(event -> {
                    if (event.getParticipantLimit() == 0) {
                        return true;
                    }
                    Long confirmedCount = requestRepository.countByEventIdAndStatus(
                            event.getId(), RequestStatus.CONFIRMED);
                    return confirmedCount < event.getParticipantLimit();
                })
                .collect(Collectors.toList());
    }

    private void saveStats(HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);

            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app(appName)
                    .uri(request.getRequestURI())
                    .ip(clientIp)
                    .timestamp(LocalDateTime.now())
                    .build();

            statsClient.saveHit(hitDto);
            log.debug("Stats saved: uri={}, ip={}", request.getRequestURI(), clientIp);
        } catch (Exception e) {
            log.error("Failed to save stats", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStatsDto> stats = statsClient.getStats(start, LocalDateTime.now(), uris, false);

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> Long.parseLong(stat.getUri().substring("/events/".length())),
                            ViewStatsDto::getHits
                    ));
        } catch (Exception e) {
            log.error("Failed to get views map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Long getViews(Event event) {
        try {
            LocalDateTime start = event.getCreatedOn();
            LocalDateTime end = LocalDateTime.now();
            String uri = "/events/" + event.getId();

            log.info("Fetching views for event {}: start={}, end={}, uri={}",
                    event.getId(), start, end, uri);

            List<ViewStatsDto> stats = statsClient.getStats(
                    start,
                    end,
                    List.of(uri),
                    false
            );

            Long views = stats.isEmpty() ? 0L : stats.get(0).getHits();
            log.info("Views for event {}: {}", event.getId(), views);

            return views;
        } catch (Exception e) {
            log.error("Failed to get views for event {}: {}", event.getId(), e.getMessage());
            return 0L;
        }
    }

    private Long extractEventId(String uri) {
        String[] parts = uri.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }
}
