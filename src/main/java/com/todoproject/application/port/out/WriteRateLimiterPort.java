package com.todoproject.application.port.out;

/**
 * Outbound port for the write rate limit (research R11).
 *
 * <p>Checked in the application core rather than in one adapter, so the HTML and REST surfaces are
 * counted together against one limit.
 */
public interface WriteRateLimiterPort {

    /**
     * Records a write attempt.
     *
     * @param clientKey an opaque key identifying the caller, typically its address
     * @return true when the write may proceed, false when the limit is already reached
     */
    boolean tryAcquire(String clientKey);
}
