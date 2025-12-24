package com.tchalanet.server.core.billing.domain.exception;

import java.util.UUID;

public class SubscriptionCannotBeResumedException extends RuntimeException {
    public SubscriptionCannotBeResumedException(UUID subscriptionId) {
        super("Subscription " + subscriptionId + " cannot be resumed");
    }
}
