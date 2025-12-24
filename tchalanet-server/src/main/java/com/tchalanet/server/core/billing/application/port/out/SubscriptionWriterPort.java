package com.tchalanet.server.core.billing.application.port.out;

import com.tchalanet.server.core.billing.domain.model.Subscription;

public interface SubscriptionWriterPort {
    Subscription save(Subscription subscription);
}
