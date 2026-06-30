package ru.practicum.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.model.EndpointHit;
import ru.practicum.model.ViewStats;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatsMapperTest {

    private StatsMapper statsMapper;

    @BeforeEach
    void setUp() {
        statsMapper = new StatsMapper();
    }

    @Test
    void toEntity_shouldMapDtoToEntity() {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 1, 15, 12, 0))
                .build();

        EndpointHit entity = statsMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(dto.getApp(), entity.getApp());
        assertEquals(dto.getUri(), entity.getUri());
        assertEquals(dto.getIp(), entity.getIp());
        assertEquals(dto.getTimestamp(), entity.getTimestamp());
        assertNull(entity.getId());
    }

    @Test
    void toDto_shouldMapEntityToDto() {
        EndpointHit entity = EndpointHit.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 1, 15, 12, 0))
                .build();

        EndpointHitDto dto = statsMapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getApp(), dto.getApp());
        assertEquals(entity.getUri(), dto.getUri());
        assertEquals(entity.getIp(), dto.getIp());
        assertEquals(entity.getTimestamp(), dto.getTimestamp());
    }

    @Test
    void toDto_shouldMapViewStatsToDto() {
        ViewStats viewStats = new ViewStats("ewm-main-service", "/events/1", 10L);

        ViewStatsDto dto = statsMapper.toDto(viewStats);

        assertNotNull(dto);
        assertEquals(viewStats.getApp(), dto.getApp());
        assertEquals(viewStats.getUri(), dto.getUri());
        assertEquals(viewStats.getHits(), dto.getHits());
    }
}