package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OfflineCodeReservationId(UUID value) {

  public OfflineCodeReservationId {
    if (value == null) {
      throw new IllegalArgumentException("OfflineCodeReservationId.value is null");
    }
  }

  public static OfflineCodeReservationId of(UUID value) {
    return new OfflineCodeReservationId(value);
  }

  public static OfflineCodeReservationId nullableOf(UUID value) {
    return value == null ? null : new OfflineCodeReservationId(value);
  }

  public static OfflineCodeReservationId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("OfflineCodeReservationId string is required");
    }
    return new OfflineCodeReservationId(UUID.fromString(raw));
  }
}
