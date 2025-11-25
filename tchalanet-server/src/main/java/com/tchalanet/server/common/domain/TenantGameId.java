package com.tchalanet.server.common.domain;

import java.util.UUID;

public record TenantGameId(UUID value) {
  public static TenantGameId of(UUID id) {
    return id == null ? null : new TenantGameId(id);
  }
}
