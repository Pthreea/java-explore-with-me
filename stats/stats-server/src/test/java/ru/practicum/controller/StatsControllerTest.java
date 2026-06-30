package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.service.StatsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatsService statsService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private EndpointHitDto hitDto;
    private ViewStatsDto viewStatsDto;

    @BeforeEach
    void setUp() {
        hitDto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2024, 1, 15, 12, 0))
                .build();

        viewStatsDto = ViewStatsDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .hits(10L)
                .build();
    }

    @Test
    void saveHit_shouldReturn201() throws Exception {
        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hitDto)))
                .andExpect(status().isCreated());

        verify(statsService, times(1)).saveHit(any(EndpointHitDto.class));
    }

    @Test
    void saveHit_shouldReturn400_whenInvalidData() throws Exception {
        EndpointHitDto invalidDto = EndpointHitDto.builder()
                .app("")
                .uri("")
                .ip("")
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_shouldReturnStats() throws Exception {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

        when(statsService.getStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of(viewStatsDto));

        mockMvc.perform(get("/stats")
                        .param("start", start.format(FORMATTER))
                        .param("end", end.format(FORMATTER))
                        .param("uris", "/events/1")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(10));

        verify(statsService, times(1)).getStats(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void getStats_shouldReturn400_whenStartMissing() throws Exception {
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

        mockMvc.perform(get("/stats")
                        .param("end", end.format(FORMATTER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_shouldReturnEmptyList_whenNoData() throws Exception {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

        when(statsService.getStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(List.of());

        mockMvc.perform(get("/stats")
                        .param("start", start.format(FORMATTER))
                        .param("end", end.format(FORMATTER)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());
    }
}