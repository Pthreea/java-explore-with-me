package ru.practicum.ewm.pub.service;

import ru.practicum.ewm.admin.dto.LocationDto;

import java.util.List;

public interface PublicLocationService {
    List<LocationDto> searchLocations(String text, int from, int size);

    LocationDto getLocationById(Long locationId);
}
