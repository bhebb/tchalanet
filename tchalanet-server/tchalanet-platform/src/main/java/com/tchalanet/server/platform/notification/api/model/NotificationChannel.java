package com.tchalanet.server.platform.notification.api.model;

/** Canaux possibles pour l'envoi d'une notification. */
public enum NotificationChannel {
  EMAIL,
  SLACK,
  SMS,
  WHATSAPP,
  PUSH,
  WEB,
  IN_APP;
}
