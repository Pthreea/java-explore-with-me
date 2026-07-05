package ru.practicum.ewm.admin.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.admin.dto.LocationDto;
import ru.practicum.ewm.admin.dto.NewLocationDto;
import ru.practicum.ewm.admin.service.AdminLocationService;

import java.util.List;

@RestController
@RequestMapping("/admin/locations")
@RequiredArgsConstructor
@Validated
public class AdminLocationController {

    private final AdminLocationService locationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDto createLocation(
            @RequestBody @Validated(NewLocationDto.Create.class) NewLocationDto dto) {
        return locationService.createLocation(dto);
    }

    @PatchMapping("/{locationId}")
    public LocationDto updateLocation(
            @PathVariable Long locationId,
            @RequestBody @Valid NewLocationDto dto) {
        return locationService.updateLocation(locationId, dto);
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable Long locationId) {
        locationService.deleteLocation(locationId);
    }

    @GetMapping("/{locationId}")
    public LocationDto getLocation(@PathVariable Long locationId) {
        return locationService.getLocationById(locationId);
    }

    @GetMapping
    public List<LocationDto> getAllLocations(
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        return locationService.getAllLocations(from, size);
    }
}
