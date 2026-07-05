package ru.practicum.ewm.util;

import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.pub.dto.LocationDto;
import ru.practicum.ewm.admin.dto.NewLocationDto;

public class LocationMapper {

    public static LocationDto toLocationDto(Location location) {
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }

    public static Location toLocation(LocationDto dto) {
        return Location.builder()
                .lat(dto.getLat())
                .lon(dto.getLon())
                .build();
    }

    public static Location toLocation(NewLocationDto dto) {
        return Location.builder()
                .name(dto.getName())
                .lat(dto.getLat())
                .lon(dto.getLon())
                .description(dto.getDescription())
                .radius(dto.getRadius() != null ? dto.getRadius() : 1000.0f)
                .build();
    }

    public static ru.practicum.ewm.admin.dto.LocationDto toAdminLocationDto(Location location) {
        return ru.practicum.ewm.admin.dto.LocationDto.builder()
                .id(location.getId())
                .name(location.getName())
                .lat(location.getLat())
                .lon(location.getLon())
                .description(location.getDescription())
                .radius(location.getRadius())
                .build();
    }
}
