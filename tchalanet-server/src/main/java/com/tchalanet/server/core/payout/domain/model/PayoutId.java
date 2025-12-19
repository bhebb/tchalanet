package com.tchalanet.server.core.payout.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Value object for Payout identifiers. */
public record PayoutId(UUID value) {
  public PayoutId {
    Objects.requireNonNull(value, "PayoutId value cannot be null");
  }
}

