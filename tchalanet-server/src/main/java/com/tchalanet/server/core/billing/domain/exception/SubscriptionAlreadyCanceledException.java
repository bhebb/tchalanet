package com.tchalanet.server.core.billing.domain.exception;

import java.util.UUID;

public class SubscriptionAlreadyCanceledException extends RuntimeException {
    public SubscriptionAlreadyCanceledException(UUID subscriptionId) {
        super("Subscription " + subscriptionId + " is already canceled");
    }
}
