package com.example.processor.repo;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.processor.model.WebEvent;

@Repository
public class EventRepository {

    private final JdbcTemplate jdbc;

    public EventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int[] batchInsert(List<WebEvent> events) {
        String sql = "INSERT INTO events (event_id, session_id, event_type, ts, payload) " +
                     "VALUES (?, ?, ?, ?, ?::jsonb) " +
                     "ON CONFLICT (event_id) DO NOTHING";

        return jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                WebEvent e = events.get(i);
                ps.setString(1, e.getEventId());
                ps.setString(2, e.getSessionId());
                ps.setString(3, e.getEventType());
                ps.setObject(4, Instant.parse(e.getTs())); // ISO-8601
                ps.setString(5, e.getPayload());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });
    }
}