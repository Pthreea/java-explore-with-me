package ru.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.model.EndpointHit;
import ru.practicum.model.ViewStats;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private StatsRepository statsRepository;

    @Mock
    private StatsMapper statsMapper;

    @InjectMocks
    private StatsServiceImpl statsService;

    private EndpointHitDto hitDto;
    private EndpointHit hit;
    private ViewStats viewStats;
    private ViewStatsDto viewStatsDto;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.of(2024, 1, 1, 0, 0);
        end = LocalDateTime.of(2024, 12, 31, 23, 59);

        hitDto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        hit = EndpointHit.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        viewStats = new ViewStats("ewm-main-service", "/events/1", 10L);

        viewStatsDto = ViewStatsDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .hits(10L)
                .build();
    }

    @Test
    void saveHit_shouldSaveSuccessfully() {
        when(statsMapper.toEntity(any(EndpointHitDto.class))).thenReturn(hit);
        when(statsRepository.save(any(EndpointHit.class))).thenReturn(hit);

        statsService.saveHit(hitDto);

        verify(statsMapper, times(1)).toEntity(hitDto);
        verify(statsRepository, times(1)).save(hit);
    }

    @Test
    void getStats_shouldReturnStats() {
        when(statsRepository.findStats(any(), any(), eq(List.of("/events/1"))))
                .thenReturn(List.of(viewStats));
        when(statsMapper.toDto(any(ViewStats.class)))
                .thenReturn(viewStatsDto);

        List<ViewStatsDto> result = statsService.getStats(start, end, List.of("/events/1"), false);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(viewStatsDto, result.get(0));
        verify(statsRepository, times(1)).findStats(start, end, List.of("/events/1"));
    }

    @Test
    void getStats_shouldReturnUniqueStats() {
        when(statsRepository.findUniqueStats(any(), any(), eq(List.of("/events/1"))))
                .thenReturn(List.of(viewStats));
        when(statsMapper.toDto(any(ViewStats.class)))
                .thenReturn(viewStatsDto);

        List<ViewStatsDto> result = statsService.getStats(start, end, List.of("/events/1"), true);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(statsRepository, times(1)).findUniqueStats(start, end, List.of("/events/1"));
    }

    @Test
    void getStats_shouldThrowException_whenStartAfterEnd() {
        LocalDateTime invalidStart = LocalDateTime.of(2024, 12, 31, 23, 59);
        LocalDateTime invalidEnd = LocalDateTime.of(2024, 1, 1, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> statsService.getStats(invalidStart, invalidEnd, null, false));
    }

    @Test
    void getStats_shouldReturnEmptyList_whenNoData() {
        when(statsRepository.findStats(any(), any(), isNull()))
                .thenReturn(List.of());

        List<ViewStatsDto> result = statsService.getStats(start, end, null, false);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(statsRepository, times(1)).findStats(start, end, null);
    }
}