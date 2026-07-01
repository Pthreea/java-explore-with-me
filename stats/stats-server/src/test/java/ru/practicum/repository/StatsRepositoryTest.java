package ru.practicum.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.model.EndpointHit;
import ru.practicum.model.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
class StatsRepositoryTest {

    @Autowired
    private StatsRepository statsRepository;

    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2024, 1, 1, 0, 0);
        end = LocalDateTime.of(2024, 12, 31, 23, 59);

        EndpointHit hit1 = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 6, 1, 12, 0))
                .build();

        EndpointHit hit2 = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.2")
                .timestamp(LocalDateTime.of(2024, 6, 2, 12, 0))
                .build();

        EndpointHit hit3 = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/2")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 6, 3, 12, 0))
                .build();

        statsRepository.saveAll(List.of(hit1, hit2, hit3));
    }

    @Test
    void findStats_shouldReturnAllHits() {
        List<ViewStats> stats = statsRepository.findStats(start, end, null);

        assertNotNull(stats);
        assertEquals(2, stats.size());
        assertEquals(2L, stats.get(0).getHits());
        assertEquals("/events/1", stats.get(0).getUri());
    }

    @Test
    void findStats_shouldFilterByUris() {
        List<ViewStats> stats = statsRepository.findStats(start, end, List.of("/events/1"));

        assertNotNull(stats);
        assertEquals(1, stats.size());
        assertEquals("/events/1", stats.get(0).getUri());
        assertEquals(2L, stats.get(0).getHits());
    }

    @Test
    void findUniqueStats_shouldReturnUniqueIps() {
        List<ViewStats> stats = statsRepository.findUniqueStats(start, end, List.of("/events/1"));

        assertNotNull(stats);
        assertEquals(1, stats.size());
        assertEquals(2L, stats.get(0).getHits());
    }

    @Test
    void findStats_shouldReturnEmptyList_whenNoDataInRange() {
        LocalDateTime futureStart = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime futureEnd = LocalDateTime.of(2025, 12, 31, 23, 59);

        List<ViewStats> stats = statsRepository.findStats(futureStart, futureEnd, null);

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }
}
