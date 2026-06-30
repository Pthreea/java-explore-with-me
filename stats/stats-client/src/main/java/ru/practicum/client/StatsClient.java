package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class StatsClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestTemplate restTemplate;

    public StatsClient(@Value("${stats-server.url}") String serverUrl, RestTemplateBuilder builder) {
        this.restTemplate = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build();
    }

    public void saveHit(EndpointHitDto hitDto) {
        try {
            HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(hitDto);
            restTemplate.exchange("/hit", HttpMethod.POST, requestEntity, Void.class);
            log.info("Hit saved: {}", hitDto);
        } catch (Exception e) {
            log.error("Error saving hit: {}", e.getMessage());
            throw new RuntimeException("Failed to save hit", e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        try {
            String encodedStart = encodeDateTime(start);
            String encodedEnd = encodeDateTime(end);

            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                    .queryParam("start", encodedStart)
                    .queryParam("end", encodedEnd)
                    .queryParam("unique", unique);

            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", String.join(",", uris));
            }

            String uri = builder.toUriString();

            ResponseEntity<ViewStatsDto[]> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    ViewStatsDto[].class
            );

            ViewStatsDto[] stats = response.getBody();
            log.info("Retrieved stats: {} records", stats != null ? stats.length : 0);
            return stats != null ? List.of(stats) : List.of();
        } catch (Exception e) {
            log.error("Error retrieving stats: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve stats", e);
        }
    }

    private String encodeDateTime(LocalDateTime dateTime) {
        String formatted = dateTime.format(FORMATTER);
        return URLEncoder.encode(formatted, StandardCharsets.UTF_8);
    }
}