package com.tchalanet.server.catalog.billing.application.port.out;

import com.tchalanet.server.catalog.billing.domain.model.Subscription;

public interface SubscriptionWriterPort {
  Subscription save(Subscription subscription);
}
