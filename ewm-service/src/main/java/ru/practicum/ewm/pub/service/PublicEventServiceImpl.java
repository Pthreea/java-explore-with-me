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
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
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

        log.info("Public search events: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort);

        // Сохраняем статистику обращения
        saveStats(request);

        // Валидация диапазона дат
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Start date must be before end date");
        }

        // Если диапазон не указан, берём от текущего момента
        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();

        // Определяем сортировку
        Pageable pageable = createPageable(from, size, sort);

        // Поиск событий
        List<Event> events = eventRepository.findPublicEvents(
                text, categories, paid, start, rangeEnd, pageable
        ).getContent();

        // Фильтрация по доступности мест
        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = filterAvailableEvents(events);
        }

        // Получаем статистику просмотров
        Map<Long, Long> viewsMap = getViewsMap(events);

        // Формируем результат
        List<EventShortDto> result = events.stream()
                .map(event -> {
                    Long confirmedRequests = requestRepository.countByEventIdAndStatus(
                            event.getId(), RequestStatus.CONFIRMED);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    return EventMapper.toEventShortDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());

        // Дополнительная сортировка по просмотрам если нужно
        if ("VIEWS".equalsIgnoreCase(sort)) {
            result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        log.info("Found {} events", result.size());
        return result;
    }

    @Override
    @Transactional
    public EventFullDto getEventById(Long id, HttpServletRequest request) {
        log.info("Getting public event by id: {}", id);

        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        // Сохраняем статистику просмотра
        saveStats(request);

        // Получаем количество подтверждённых заявок
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(id, RequestStatus.CONFIRMED);

        // Получаем количество просмотров
        Long views = getViews(event);

        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    private Pageable createPageable(int from, int size, String sort) {
        if ("EVENT_DATE".equalsIgnoreCase(sort)) {
            return PageRequest.of(from / size, size, Sort.by("eventDate").ascending());
        }
        // По умолчанию сортировка по ID
        return PageRequest.of(from / size, size);
    }

    private List<Event> filterAvailableEvents(List<Event> events) {
        return events.stream()
                .filter(event -> {
                    if (event.getParticipantLimit() == 0) {
                        return true; // Нет лимита
                    }
                    Long confirmedCount = requestRepository.countByEventIdAndStatus(
                            event.getId(), RequestStatus.CONFIRMED);
                    return confirmedCount < event.getParticipantLimit();
                })
                .collect(Collectors.toList());
    }

    private void saveStats(HttpServletRequest request) {
        try {
            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app(appName)
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();

            statsClient.saveHit(hitDto);
            log.debug("Stats saved: uri={}, ip={}", request.getRequestURI(), request.getRemoteAddr());
        } catch (Exception e) {
            log.error("Failed to save stats", e);
        }
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> uris = events.stream()
                    .map(event -> "/events/" + event.getId())
                    .collect(Collectors.toList());

            LocalDateTime start = events.stream()
                    .map(Event::getCreatedOn)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now().minusYears(100));

            List<ViewStatsDto> stats = statsClient.getStats(
                    start,
                    LocalDateTime.now(),
                    uris,
                    true
            );

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> extractEventId(stat.getUri()),
                            ViewStatsDto::getHits,
                            (a, b) -> a
                    ));
        } catch (Exception e) {
            log.error("Failed to get views stats", e);
            return Collections.emptyMap();
        }
    }

    private Long getViews(Event event) {
        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    event.getCreatedOn(),
                    LocalDateTime.now(),
                    List.of("/events/" + event.getId()),
                    true
            );

            return stats.isEmpty() ? 0L : stats.get(0).getHits();
        } catch (Exception e) {
            log.error("Failed to get views for event {}", event.getId(), e);
            return 0L;
        }
    }

    private Long extractEventId(String uri) {
        // Извлекаем ID события из URI вида "/events/123"
        String[] parts = uri.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }
}
