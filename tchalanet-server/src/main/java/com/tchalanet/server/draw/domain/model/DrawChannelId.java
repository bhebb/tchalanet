package com.tchalanet.server.draw.domain.model;

import java.util.UUID;

public record DrawChannelId(UUID value) {
  public DrawChannelId {
    if (value == null) {
      throw new IllegalArgumentException("DrawChannelId cannot be null");
    }
  }

  public static DrawChannelId generate() {
    return new DrawChannelId(UUID.randomUUID());
  }
}
