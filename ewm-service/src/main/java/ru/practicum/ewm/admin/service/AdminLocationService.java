package ru.practicum.ewm.admin.service;

import ru.practicum.ewm.admin.dto.LocationDto;
import ru.practicum.ewm.admin.dto.NewLocationDto;

import java.util.List;

public interface AdminLocationService {
    LocationDto createLocation(NewLocationDto dto);
    LocationDto updateLocation(Long locationId, NewLocationDto dto);
    void deleteLocation(Long locationId);
    LocationDto getLocationById(Long locationId);
    List<LocationDto> getAllLocations(int from, int size);
}
