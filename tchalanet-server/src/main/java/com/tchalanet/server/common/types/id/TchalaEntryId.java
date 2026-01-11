package com.tchalanet.server.common.types.id;

import java.util.Objects;
import java.util.UUID;

public record TchalaEntryId(UUID value) {
  public static TchalaEntryId newId() {
    return new TchalaEntryId(UUID.randomUUID());
  }

  public static TchalaEntryId of(UUID id) {
    Objects.requireNonNull(id);
    return new TchalaEntryId(id);
  }
}
