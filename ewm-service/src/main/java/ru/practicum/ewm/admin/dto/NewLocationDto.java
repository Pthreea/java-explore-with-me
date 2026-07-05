package ru.practicum.ewm.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewLocationDto {
    @NotBlank
    @Size(min = 3, max = 255)
    private String name;

    @NotNull
    private Float lat;

    @NotNull
    private Float lon;

    @Size(max = 1000)
    private String description;

    @Positive
    private Float radius;
}