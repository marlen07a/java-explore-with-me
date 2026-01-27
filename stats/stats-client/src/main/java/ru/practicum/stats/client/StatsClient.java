package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class StatsClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestTemplate restTemplate;

    public StatsClient(@Value("${stats.server.url:http://localhost:9090}") String serverUrl,
                       RestTemplateBuilder builder) {
        this.restTemplate = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build();
    }

    public void saveHit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();

        log.info("Отправка статистики: app={}, uri={}, ip={}", app, uri, ip);

        try {
            restTemplate.postForEntity("/hit", new HttpEntity<>(hitDto), EndpointHitDto.class);
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики: {}", e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        log.info("Запрос статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        String startStr = start.format(FORMATTER);
        String endStr = end.format(FORMATTER);

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", startStr)
                .queryParam("end", endStr);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }

        if (unique != null) {
            builder.queryParam("unique", unique);
        }

        try {
            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    builder.build().toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<ViewStatsDto> body = response.getBody();
            return body != null ? body : List.of();
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            return List.of();
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end) {
        return getStats(start, end, null, false);
    }
}
