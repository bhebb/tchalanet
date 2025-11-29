package com.tchalanet.server.core.tenant.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object for a TenantGame's unique identifier. */
public record TenantGameId(UUID value) {
  public TenantGameId {
    Objects.requireNonNull(value, "TenantGameId value cannot be null");
  }
}
