package com.tchalanet.server.common.domain;

import java.util.UUID;

public record PlanId(UUID value) {
  public static PlanId of(UUID id) {
    return id == null ? null : new PlanId(id);
  }
}
