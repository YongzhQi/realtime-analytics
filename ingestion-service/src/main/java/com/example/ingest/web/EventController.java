package com.example.ingest.web;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ingest.model.WebEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/events")
public class EventController {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventController(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> ingest(@Valid @RequestBody WebEvent e) throws Exception {
        if (e.getEventId() == null || e.getEventId().isBlank()) e.setEventId(UUID.randomUUID().toString());
        if (e.getTs() == null || e.getTs().isBlank()) e.setTs(Instant.now().toString());

        String json = objectMapper.writeValueAsString(e);
        kafkaTemplate.send("events", e.getSessionId(), json);
        return ResponseEntity.accepted().build();
    }
}