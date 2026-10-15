package com.todoproject.adapter.out.limit;

import static org.assertj.core.api.Assertions.assertThat;

import com.todoproject.TaskCardFixtures;
import com.todoproject.config.BoardProperties;
import org.junit.jupiter.api.Test;

/**
 * Covers the window boundary and reset in research R11.
 *
 * <p>Same package as {@link InMemoryWriteRateLimiter} by convention, matching how the other
 * adapter tests are organized.
 */
class InMemoryWriteRateLimiterTest {

    @Test
    void allowsUpToTheConfiguredLimitWithinOneWindow() {
        TaskCardFixtures.FixedClock clock = new TaskCardFixtures.FixedClock();
        InMemoryWriteRateLimiter limiter = newLimiter(clock, 3);

        assertThat(limiter.tryAcquire("client-1")).isTrue();
        assertThat(limiter.tryAcquire("client-1")).isTrue();
        assertThat(limiter.tryAcquire("client-1")).isTrue();
        assertThat(limiter.tryAcquire("client-1")).isFalse();
    }

    @Test
    void resetsAfterTheWindowElapses() {
        TaskCardFixtures.FixedClock clock = new TaskCardFixtures.FixedClock();
        InMemoryWriteRateLimiter limiter = newLimiter(clock, 1);

        assertThat(limiter.tryAcquire("client-1")).isTrue();
        assertThat(limiter.tryAcquire("client-1")).isFalse();

        clock.advanceTo(TaskCardFixtures.FIXED_NOW.plusSeconds(61));

        assertThat(limiter.tryAcquire("client-1")).isTrue();
    }

    @Test
    void tracksEachClientIndependently() {
        TaskCardFixtures.FixedClock clock = new TaskCardFixtures.FixedClock();
        InMemoryWriteRateLimiter limiter = newLimiter(clock, 1);

        assertThat(limiter.tryAcquire("client-1")).isTrue();
        assertThat(limiter.tryAcquire("client-2")).isTrue();
        assertThat(limiter.tryAcquire("client-1")).isFalse();
    }

    @Test
    void treatsABlankClientKeyAsOneSharedBucket() {
        TaskCardFixtures.FixedClock clock = new TaskCardFixtures.FixedClock();
        InMemoryWriteRateLimiter limiter = newLimiter(clock, 1);

        assertThat(limiter.tryAcquire(null)).isTrue();
        assertThat(limiter.tryAcquire("")).isFalse();
    }

    private static InMemoryWriteRateLimiter newLimiter(TaskCardFixtures.FixedClock clock, int writesPerWindow) {
        BoardProperties properties =
                new BoardProperties(new BoardProperties.Board(500), new BoardProperties.Limits(writesPerWindow));
        return new InMemoryWriteRateLimiter(clock, properties);
    }
}
