package com.tchalanet.server.tenant.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object for a Subscription's unique identifier. */
public record SubscriptionId(UUID value) {
  public SubscriptionId {
    Objects.requireNonNull(value, "SubscriptionId value cannot be null");
  }
}
