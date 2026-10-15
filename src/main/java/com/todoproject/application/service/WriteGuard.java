package com.todoproject.application.service;

import com.todoproject.application.port.out.WriteRateLimiterPort;
import com.todoproject.domain.error.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The write rate limit, applied in the core so both inbound surfaces are counted together
 * (research R11), plus the content-free security logging the constitution requires (research R12).
 *
 * <p>Nothing logged here can carry a card's title or note: the signature only accepts an action
 * name, a card identifier and the caller key.
 */
public final class WriteGuard {

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("com.todoproject.security");

    private final WriteRateLimiterPort rateLimiter;

    public WriteGuard(WriteRateLimiterPort rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * @throws RateLimitExceededException when the caller has already used its allowance
     */
    void requireWriteAllowed(String action, String clientKey) {
        if (!rateLimiter.tryAcquire(clientKey)) {
            SECURITY_LOG.info("event=rate_limited action={} client={}", action, clientKey);
            throw new RateLimitExceededException();
        }
    }

    void logRefused(String action, String reason, String cardId, String clientKey) {
        SECURITY_LOG.info("event=refused action={} reason={} card={} client={}", action, reason, cardId, clientKey);
    }

    void logApplied(String action, String cardId, String detail) {
        SECURITY_LOG.info("event=applied action={} card={} detail={}", action, cardId, detail);
    }
}
