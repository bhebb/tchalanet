package com.tchalanet.server.platform.notification.api.model;

import java.util.UUID;

public record NotificationPublicationId(UUID value) {

  public NotificationPublicationId {
    if (value == null) {
      throw new IllegalArgumentException("notification publication id is required");
    }
  }

  public static NotificationPublicationId of(UUID value) {
    return new NotificationPublicationId(value);
  }

  public static NotificationPublicationId nullableOf(UUID value) {
    return value == null ? null : new NotificationPublicationId(value);
  }
}
