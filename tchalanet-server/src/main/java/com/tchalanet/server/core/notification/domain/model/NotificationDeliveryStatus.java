package com.tchalanet.server.core.notification.domain.model;

public enum NotificationDeliveryStatus {
  PENDING,
  SENT,
  DELIVERED,
  FAILED,
  SKIPPED,
  CANCELLED
}
