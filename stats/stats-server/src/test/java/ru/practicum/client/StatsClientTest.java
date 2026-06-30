package ru.practicum.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import ru.practicum.dto.EndpointHitDto;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatsClientTest {

    private StatsClient statsClient;

    @BeforeEach
    void setUp() {
        statsClient = new StatsClient("http://localhost:9090", new RestTemplateBuilder());
    }

    @Test
    void statsClient_shouldBeCreated() {
        assertNotNull(statsClient);
    }

    @Test
    void saveHit_shouldCreateValidDto() {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        assertNotNull(hitDto);
        assertNotNull(hitDto.getApp());
        assertNotNull(hitDto.getUri());
        assertNotNull(hitDto.getIp());
        assertNotNull(hitDto.getTimestamp());
    }
}