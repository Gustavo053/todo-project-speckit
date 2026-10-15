package com.todoproject.application.port.out;

import java.time.Instant;

/** Outbound port for the current time, so use case tests are deterministic. */
public interface ClockPort {

    Instant now();
}
