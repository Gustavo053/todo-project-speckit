package com.todoproject.application;

import com.todoproject.application.port.out.WriteRateLimiterPort;

/** A rate limiter test double that never refuses, for tests that are not about the limit. */
public class AlwaysAllowRateLimiter implements WriteRateLimiterPort {

    @Override
    public boolean tryAcquire(String clientKey) {
        return true;
    }
}
