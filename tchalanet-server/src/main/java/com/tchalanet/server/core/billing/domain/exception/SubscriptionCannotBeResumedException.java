package com.tchalanet.server.core.billing.domain.exception;

import com.tchalanet.server.common.types.id.SubscriptionId;

public class SubscriptionCannotBeResumedException extends RuntimeException {
    public SubscriptionCannotBeResumedException(SubscriptionId subscriptionId) {
        super("Subscription " + subscriptionId + " cannot be resumed");
    }
}
