package ru.practicum.client;

import java.util.Arrays;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

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

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String startStr = start.format(FORMATTER);
        String endStr = end.format(FORMATTER);

        String url = "/stats?start={start}&end={end}&unique={unique}";

        if (uris != null && !uris.isEmpty()) {
            String urisParam = String.join(",", uris);
            url += "&uris={uris}";

            try {
                ResponseEntity<ViewStatsDto[]> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        ViewStatsDto[].class,
                        startStr,
                        endStr,
                        unique,
                        urisParam
                );

                return response.getBody() != null ? Arrays.asList(response.getBody()) : Collections.emptyList();
            } catch (Exception e) {
                log.error("Error retrieving stats: {}", e.getMessage());
                throw new RuntimeException("Failed to retrieve stats", e);
            }
        }

        try {
            ResponseEntity<ViewStatsDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    ViewStatsDto[].class,
                    startStr,
                    endStr,
                    unique
            );

            return response.getBody() != null ? Arrays.asList(response.getBody()) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error retrieving stats: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve stats", e);
        }
    }
}