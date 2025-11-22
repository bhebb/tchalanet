package com.tchalanet.server.tenant.domain.model;

import java.util.UUID;

/** Identifiant de tenant côté domaine (value object). */
public record TenantId(UUID value) {

  public TenantId {
    if (value == null) {
      throw new IllegalArgumentException("TenantId cannot be null");
    }
  }
}
