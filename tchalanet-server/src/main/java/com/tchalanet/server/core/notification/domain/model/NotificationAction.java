package com.tchalanet.server.core.notification.domain.model;

public record NotificationAction(String type, String url) {

  public boolean isEmpty() {
    return (type == null || type.isBlank()) && (url == null || url.isBlank());
  }
}
