package com.tchalanet.server.common.domain;

import java.util.UUID;

public record TenantId(UUID value) {
  public static TenantId of(UUID id) {
    return id == null ? null : new TenantId(id);
  }
}
