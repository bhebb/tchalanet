package com.tchalanet.server.core.accesscontrol.domain.model;

import java.util.Objects;

/** Value Object représentant une permission métier. Exemple : "ticket.create", "draw.override". */
public record Permission(String key) {

  public Permission {
    Objects.requireNonNull(key, "Permission key cannot be null");
    if (key.isBlank()) {
      throw new IllegalArgumentException("Permission key cannot be blank");
    }
  }

  @Override
  public String toString() {
    return key;
  }
}
