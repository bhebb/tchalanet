package com.tchalanet.server.core.billing.domain.exception;

import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.core.billing.domain.model.SubscriptionStatus;

public class SubscriptionCannotBeCanceledException extends RuntimeException {
    public SubscriptionCannotBeCanceledException(SubscriptionId subscriptionId, SubscriptionStatus status) {
        super("Subscription " + subscriptionId + " with status " + status + " cannot be canceled");
    }
}
