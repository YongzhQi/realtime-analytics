package com.example.processor.kafka;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.processor.metrics.MetricsService;
import com.example.processor.model.WebEvent;
import com.example.processor.repo.EventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final EventRepository repository;
    private final ObjectMapper objectMapper;
    private final MetricsService metrics;

    public EventConsumer(EventRepository repository, ObjectMapper objectMapper, MetricsService metrics) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    // Be explicit about which container factory to use
    @KafkaListener(
        topics = "events",
        groupId = "processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessages(List<ConsumerRecord<String, String>> records) throws Exception {
        if (records == null || records.isEmpty()) {
            return;
        }

        long start = System.nanoTime();
        log.info("Received batch with {} record(s). First offset: {}",
                records.size(), records.get(0).offset());

        List<WebEvent> batch = new ArrayList<>(records.size());
        for (ConsumerRecord<String, String> rec : records) {
            String json = rec.value();
            JsonNode node = objectMapper.readTree(json);

            WebEvent e = new WebEvent();
            e.setEventId(node.hasNonNull("eventId") ? node.get("eventId").asText() : UUID.randomUUID().toString());
            e.setSessionId(node.hasNonNull("sessionId") ? node.get("sessionId").asText() : "unknown");
            e.setEventType(node.hasNonNull("eventType") ? node.get("eventType").asText() : "unknown");
            e.setTs(node.hasNonNull("ts") ? node.get("ts").asText() : Instant.now().toString());
            e.setPayload(json);

            batch.add(e);
        }

        int[] results = repository.batchInsert(batch);
        int written = 0;
        for (int r : results) {
            if (r >= 0) written += r;
        }

        long nanos = System.nanoTime() - start;
        metrics.recordBatch(records.size(), written, nanos);
        metrics.updateLag(-1L); // TODO: replace with real lag if needed

        log.info("Processed batch: received={}, inserted={}, took={} ms",
                records.size(), written, nanos / 1_000_000);
    }
}