package com.tchalanet.server.core.draw.domain.model;

/**
 * Lifecycle status for a tenant draw.
 *
 * <p>Main flow:
 * SCHEDULED -> OPEN -> CLOSED -> RESULTED -> SETTLED -> ARCHIVED
 *
 * <p>Cancellation flow:
 * SCHEDULED|OPEN|CLOSED -> CANCELED -> ARCHIVED
 */
public enum DrawStatus {
    SCHEDULED,
    OPEN,
    CLOSED,
    RESULTED,
    SETTLED,
    CANCELED,
    ARCHIVED;

    public boolean isActive() {
        return this == SCHEDULED || this == OPEN;
    }

    public boolean isTerminal() {
        return this == SETTLED || this == CANCELED || this == ARCHIVED;
    }

    public boolean canBeCancelled() {
        return this == SCHEDULED || this == OPEN || this == CLOSED;
    }
}
