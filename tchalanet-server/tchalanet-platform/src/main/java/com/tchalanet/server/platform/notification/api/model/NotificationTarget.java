package com.tchalanet.server.platform.notification.api.model;

import java.util.UUID;

public record NotificationTarget(NotificationActorType actorType, UUID actorId) {

  public NotificationTarget {
    if (actorType == null) {
      throw new IllegalArgumentException("notification target actor type is required");
    }
    if (actorId == null) {
      throw new IllegalArgumentException("notification target actor id is required");
    }
  }

  public static NotificationTarget appUser(UUID actorId) {
    return new NotificationTarget(NotificationActorType.APP_USER, actorId);
  }

  public static NotificationTarget sellerTerminal(UUID actorId) {
    return new NotificationTarget(NotificationActorType.SELLER_TERMINAL, actorId);
  }
}
