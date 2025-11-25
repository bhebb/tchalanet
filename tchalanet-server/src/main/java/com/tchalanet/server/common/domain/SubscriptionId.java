package com.tchalanet.server.common.domain;

import java.util.UUID;

public record SubscriptionId(UUID value) {
  public static SubscriptionId of(UUID id) {
    return id == null ? null : new SubscriptionId(id);
  }
}
