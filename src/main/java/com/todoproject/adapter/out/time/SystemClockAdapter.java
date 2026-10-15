package com.todoproject.adapter.out.time;

import com.todoproject.application.port.out.ClockPort;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** The real clock. Tests substitute a fixed one through the port. */
@Component
public class SystemClockAdapter implements ClockPort {

    private final Clock clock = Clock.systemUTC();

    @Override
    public Instant now() {
        return clock.instant();
    }
}
