package com.todoproject.application;

import com.todoproject.application.port.out.WriteRateLimiterPort;

/** A rate limiter test double that always refuses, for exercising the refusal path. */
public class NeverAllowRateLimiter implements WriteRateLimiterPort {

    @Override
    public boolean tryAcquire(String clientKey) {
        return false;
    }
}
