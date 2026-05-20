package com.tchalanet.server.platform.notification.internal.service;

public record NotificationAction(String type, String url) {

  public boolean isEmpty() {
    return (type == null || type.isBlank()) && (url == null || url.isBlank());
  }
}
