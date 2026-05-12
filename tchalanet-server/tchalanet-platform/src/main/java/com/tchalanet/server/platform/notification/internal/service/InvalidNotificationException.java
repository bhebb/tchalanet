package com.tchalanet.server.platform.notification.internal.service;

public class InvalidNotificationException extends RuntimeException {
  public InvalidNotificationException(String message) {
    super(message);
  }
}
