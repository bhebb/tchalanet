package com.tchalanet.server.core.billing.domain.model;

import java.time.Instant;

public enum SubscriptionStatus {
    ACTIVE,
    TRIALING,
    CANCELED,
    PAST_DUE,
    SUSPENDED;

    public boolean isCancelable() {
        return this == TRIALING || this == ACTIVE || this == SUSPENDED;
    }
}
