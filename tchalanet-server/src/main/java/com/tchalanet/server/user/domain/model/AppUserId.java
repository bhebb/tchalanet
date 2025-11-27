package com.tchalanet.server.user.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object for an AppUser's unique identifier. */
public record AppUserId(UUID value) {
  public AppUserId {
    Objects.requireNonNull(value, "AppUserId value cannot be null");
  }
}
