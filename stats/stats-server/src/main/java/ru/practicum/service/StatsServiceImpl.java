package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.model.EndpointHit;
import ru.practicum.model.ViewStats;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final StatsMapper statsMapper;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hitDto) {
        EndpointHit hit = statsMapper.toEntity(hitDto);
        EndpointHit savedHit = statsRepository.save(hit);
        log.info("Saved hit: {}", savedHit);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        List<ViewStats> stats;
        if (unique) {
            stats = statsRepository.findUniqueStats(start, end, uris);
            log.info("Retrieved unique stats from {} to {}: {} records", start, end, stats.size());
        } else {
            stats = statsRepository.findStats(start, end, uris);
            log.info("Retrieved stats from {} to {}: {} records", start, end, stats.size());
        }

        return stats.stream()
                .map(statsMapper::toDto)
                .toList();
    }
}