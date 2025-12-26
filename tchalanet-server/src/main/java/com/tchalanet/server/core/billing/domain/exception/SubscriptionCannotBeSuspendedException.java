package com.tchalanet.server.core.billing.domain.exception;

import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.core.billing.domain.model.SubscriptionStatus;

import java.util.UUID;

public class SubscriptionCannotBeSuspendedException extends RuntimeException {
    public SubscriptionCannotBeSuspendedException(SubscriptionId subscriptionId, SubscriptionStatus status) {
        super("Subscription " + subscriptionId + " with status " + status + " cannot be suspended");
    }
}
