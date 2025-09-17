package com.example.ingest.model;

import jakarta.validation.constraints.NotBlank;

public class WebEvent {
    private String eventId;
    @NotBlank
    private String sessionId;
    @NotBlank
    private String eventType;
    @NotBlank
    private String payload; // raw JSON as string
    private String ts; // ISO-8601

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getTs() { return ts; }
    public void setTs(String ts) { this.ts = ts; }
}