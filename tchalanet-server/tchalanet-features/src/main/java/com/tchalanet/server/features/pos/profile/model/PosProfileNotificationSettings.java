package com.tchalanet.server.features.pos.profile.model;

public record PosProfileNotificationSettings(boolean enabled, boolean criticalOnly) {
  public static PosProfileNotificationSettings defaults() {
    return new PosProfileNotificationSettings(true, false);
  }
}
