package com.todoproject.adapter.out.limit;

import com.todoproject.application.port.out.ClockPort;
import com.todoproject.application.port.out.WriteRateLimiterPort;
import com.todoproject.config.BoardProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Fixed-window write limiter, held in memory (research R11).
 *
 * <p>Deliberately simple: the deployment is single-user and local, so the point is to give the
 * boundary a defined behaviour rather than to be a distributed limiter. A new dependency for this
 * would need justification under the constitution's dependency rule.
 */
@Component
public class InMemoryWriteRateLimiter implements WriteRateLimiterPort {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ClockPort clock;
    private final int writesPerWindow;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    InMemoryWriteRateLimiter(ClockPort clock, BoardProperties properties) {
        this.clock = clock;
        this.writesPerWindow = properties.writesPerMinute();
    }

    @Override
    public boolean tryAcquire(String clientKey) {
        String key = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
        Instant now = clock.now();
        Window updated = windows.compute(key, (ignored, current) -> {
            if (current == null || now.isAfter(current.startedAt().plus(WINDOW))) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.count() + 1);
        });
        return updated.count() <= writesPerWindow;
    }

    private record Window(Instant startedAt, int count) {}
}
