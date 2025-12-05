package com.tchalanet.server.core.notification.domain;

/**
 * Types métier de notifications.
 */
public enum NotificationType {
  /** Envoi d'un reçu de ticket. */
  TICKET_RECEIPT,

  /** Alerte limite atteinte / dépassée. */
  LIMIT_ALERT,

  /** Message système / info plateforme. */
  SYSTEM_MESSAGE,

  /** Type libre pour des usages futurs. */
  CUSTOM
}

