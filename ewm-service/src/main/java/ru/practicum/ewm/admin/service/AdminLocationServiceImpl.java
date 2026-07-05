package ru.practicum.ewm.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.admin.dto.LocationDto;
import ru.practicum.ewm.admin.dto.NewLocationDto;
import ru.practicum.ewm.exception.ConflictException;
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
public class AdminLocationServiceImpl implements AdminLocationService {

    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public LocationDto createLocation(NewLocationDto dto) {
        log.info("Creating location: {}", dto.getName());

        if (locationRepository.existsByName(dto.getName())) {
            throw new ConflictException("Location with name '" + dto.getName() + "' already exists");
        }

        Location location = LocationMapper.toLocation(dto);
        Location saved = locationRepository.save(location);

        log.info("Location created with id: {}", saved.getId());
        return LocationMapper.toAdminLocationDto(saved);
    }

    @Override
    @Transactional
    public LocationDto updateLocation(Long locationId, NewLocationDto dto) {
        log.info("Updating location {}: {}", locationId, dto.getName());

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found"));

        if (dto.getName() != null && !dto.getName().equals(location.getName())) {
            if (locationRepository.existsByName(dto.getName())) {
                throw new ConflictException("Location with name '" + dto.getName() + "' already exists");
            }
            location.setName(dto.getName());
        }

        if (dto.getLat() != null) {
            location.setLat(dto.getLat());
        }
        if (dto.getLon() != null) {
            location.setLon(dto.getLon());
        }
        if (dto.getDescription() != null) {
            location.setDescription(dto.getDescription());
        }
        if (dto.getRadius() != null) {
            location.setRadius(dto.getRadius());
        }

        Location updated = locationRepository.save(location);
        log.info("Location {} updated", locationId);
        return LocationMapper.toAdminLocationDto(updated);
    }

    @Override
    @Transactional
    public void deleteLocation(Long locationId) {
        log.info("Deleting location {}", locationId);

        if (!locationRepository.existsById(locationId)) {
            throw new NotFoundException("Location not found");
        }

        locationRepository.deleteById(locationId);
        log.info("Location {} deleted", locationId);
    }

    @Override
    public LocationDto getLocationById(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found"));
        return LocationMapper.toAdminLocationDto(location);
    }

    @Override
    public List<LocationDto> getAllLocations(int from, int size) {
        return locationRepository.findAll(PageRequest.of(from / size, size))
                .stream()
                .map(LocationMapper::toAdminLocationDto)
                .collect(Collectors.toList());
    }
}