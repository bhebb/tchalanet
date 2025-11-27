package com.tchalanet.server.draw.domain.model;

import java.util.UUID;

public final class TenantId {
  private final UUID id;

  public TenantId(UUID id) {
    this.id = id;
  }

  public UUID value() {
    return id;
  }
}
