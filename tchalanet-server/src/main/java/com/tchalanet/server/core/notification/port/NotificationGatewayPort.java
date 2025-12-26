package com.tchalanet.server.core.notification.port;

import com.tchalanet.server.core.notification.domain.SendNotificationPayload;

/** Port d'accès vers le service externe de notifications (serveur Node). */
public interface NotificationGatewayPort {

  /**
   * Envoie une notification via le service externe.
   *
   * @param payload description générique de la notification à envoyer
   */
  void send(SendNotificationPayload payload);
}
