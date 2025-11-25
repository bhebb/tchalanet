package com.tchalanet.server.common.domain;

import java.util.UUID;

public record DrawChannelId(UUID value) {
  public static DrawChannelId of(UUID id) {
    return id == null ? null : new DrawChannelId(id);
  }
}
