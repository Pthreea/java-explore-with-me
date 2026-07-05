package ru.practicum.ewm.pub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.admin.dto.LocationDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.repository.LocationRepository;
import ru.practicum.ewm.util.LocationMapper;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicLocationServiceImpl implements PublicLocationService {

    private final LocationRepository locationRepository;

    @Override
    public List<LocationDto> searchLocations(String text, int from, int size) {
        log.info("Searching locations: text={}, from={}, size={}", text, from, size);

        Pageable pageable = PageRequest.of(from / size, size);

        List<Location> locations;
        if (text == null || text.isBlank()) {
            locations = locationRepository.findAll(pageable).getContent();
        } else {
            locations = locationRepository.searchByName(text, pageable).getContent();
        }

        log.info("Found {} locations", locations.size());
        return locations.stream()
                .map(LocationMapper::toAdminLocationDto)
                .collect(Collectors.toList());
    }

    @Override
    public LocationDto getLocationById(Long locationId) {
        log.info("Getting location {}", locationId);

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location with id=" + locationId + " was not found"));

        return LocationMapper.toAdminLocationDto(location);
    }
}
