package com.example.processor.metrics;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class MetricsService {
    private final Counter received;
    private final Counter written;
    private final Timer processingTimer;
    private final AtomicLong lastLag = new AtomicLong(-1);

    public MetricsService(MeterRegistry registry) {
        this.received = Counter.builder("events.received.total").register(registry);
        this.written = Counter.builder("events.written.total").register(registry);
        this.processingTimer = Timer.builder("events.processing.time").register(registry);
    }

    public void recordBatch(int receivedCount, int writtenCount, long nanos) {
        if (receivedCount > 0) received.increment(receivedCount);
        if (writtenCount > 0) written.increment(writtenCount);
        processingTimer.record(Duration.ofNanos(nanos));
    }

    public void updateLag(long lag) {
        lastLag.set(lag);
    }

    // Accessors used by SSE controller
    public double getReceived() {
        return received.count();
    }

    public double getWritten() {
        return written.count();
    }

    public double getAvgProcessingMs() {
        long count = processingTimer.count();
        if (count == 0) return 0.0;
        return processingTimer.totalTime(TimeUnit.MILLISECONDS) / count;
    }

    public long getLag() {
        return lastLag.get();
    }
}