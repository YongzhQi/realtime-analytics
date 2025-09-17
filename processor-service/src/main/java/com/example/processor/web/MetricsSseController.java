package com.example.processor.web;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.processor.metrics.MetricsService;

@RestController
public class MetricsSseController {

    private final MetricsService metrics;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public MetricsSseController(MetricsService metrics) {
        this.metrics = metrics;
    }

    @GetMapping(path = "/metrics/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(0L);

        ScheduledFuture<?> handle = scheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> payload = Map.of(
                        "receivedTotal", metrics.getReceived(),
                        "writtenTotal", metrics.getWritten(),
                        "avgProcessingMs", metrics.getAvgProcessingMs(),
                        "lag", metrics.getLag(),
                        "ts", java.time.Instant.now().toString()
                );
                emitter.send(SseEmitter.event().name("metrics").data(payload));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }, 0, 1, TimeUnit.SECONDS);

        emitter.onCompletion(() -> handle.cancel(false));
        emitter.onTimeout(() -> handle.cancel(false));
        return emitter;
    }
}