package com.todoproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits on an unauthenticated write surface (research R11).
 *
 * @param maxCards how many cards the single shared board may hold
 * @param writesPerMinute write actions allowed per client address per minute
 */
@ConfigurationProperties(prefix = "todo")
public record BoardProperties(Board board, Limits limits) {

    public BoardProperties {
        board = board == null ? new Board(500) : board;
        limits = limits == null ? new Limits(60) : limits;
    }

    public int maxCards() {
        return board.maxCards();
    }

    public int writesPerMinute() {
        return limits.writesPerMinute();
    }

    public record Board(int maxCards) {
        public Board {
            if (maxCards < 1) {
                throw new IllegalArgumentException("todo.board.max-cards must be at least 1");
            }
        }
    }

    public record Limits(int writesPerMinute) {
        public Limits {
            if (writesPerMinute < 1) {
                throw new IllegalArgumentException("todo.limits.writes-per-minute must be at least 1");
            }
        }
    }
}
