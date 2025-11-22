package com.tchalanet.server.user.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Identifiant utilisateur côté domaine. */
public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "UserId value must not be null");
  }
}
