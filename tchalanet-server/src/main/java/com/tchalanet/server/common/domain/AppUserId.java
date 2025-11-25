package com.tchalanet.server.common.domain;

import java.util.UUID;

public record AppUserId(UUID value) {
  public static AppUserId of(UUID id) {
    return id == null ? null : new AppUserId(id);
  }
}
