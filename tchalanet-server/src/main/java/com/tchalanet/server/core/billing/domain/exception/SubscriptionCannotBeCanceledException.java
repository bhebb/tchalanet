package com.tchalanet.server.core.billing.domain.exception;

import com.tchalanet.server.core.billing.domain.model.SubscriptionStatus;

import java.util.UUID;

public class SubscriptionCannotBeCanceledException extends RuntimeException {
    public SubscriptionCannotBeCanceledException(UUID subscriptionId, SubscriptionStatus status) {
        super("Subscription " + subscriptionId + " with status " + status + " cannot be canceled");
    }
}
