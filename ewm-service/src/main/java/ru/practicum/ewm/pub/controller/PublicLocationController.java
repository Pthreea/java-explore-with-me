package ru.practicum.ewm.pub.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.admin.dto.LocationDto;
import ru.practicum.ewm.pub.service.PublicLocationService;

import java.util.List;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Validated
public class PublicLocationController {

    private final PublicLocationService locationService;

    @GetMapping
    public List<LocationDto> searchLocations(
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        return locationService.searchLocations(text, from, size);
    }

    @GetMapping("/{locationId}")
    public LocationDto getLocation(@PathVariable Long locationId) {
        return locationService.getLocationById(locationId);
    }
}
