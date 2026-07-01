package ru.practicum.ewm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.client.StatsClient;

@Configuration
public class StatsClientConfig {

    @Value("${stats-server.url}")
    private String statsServerUrl;

    @Bean
    public StatsClient statsClient(RestTemplateBuilder builder) {
        return new StatsClient(statsServerUrl, builder);
    }
}