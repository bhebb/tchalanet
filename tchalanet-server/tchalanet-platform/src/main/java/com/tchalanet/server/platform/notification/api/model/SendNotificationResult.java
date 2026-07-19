package com.tchalanet.server.platform.notification.api.model;

/** Result of an accepted notification request. User-facing copy is resolved by the caller. */
public record SendNotificationResult(boolean success, String code, String idempotencyKey) {
  public static SendNotificationResult accepted(String idempotencyKey) {
    return new SendNotificationResult(true, "notification.accepted", idempotencyKey);
  }
}
