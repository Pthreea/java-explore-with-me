package ru.practicum.ewm.priv.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {
    @NotEmpty(message = "Request IDs must not be empty")
    private List<Long> requestIds;

    @NotNull(message = "Status must not be null")
    private String status;
}
