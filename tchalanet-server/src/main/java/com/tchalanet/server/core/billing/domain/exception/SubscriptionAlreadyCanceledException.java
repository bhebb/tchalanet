package com.tchalanet.server.core.billing.domain.exception;

import com.tchalanet.server.common.types.id.SubscriptionId;

public class SubscriptionAlreadyCanceledException extends RuntimeException {
  public SubscriptionAlreadyCanceledException(SubscriptionId subscriptionId) {
    super("Subscription " + subscriptionId + " is already canceled");
  }
}
