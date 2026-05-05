package com.tchalanet.server.common.notification;

import com.tchalanet.server.common.notification.model.SendNotificationPayload;

/** Port d'accès vers le service externe de notifications (tchalanet-edge-service). */
public interface NotificationGatewayPort {

  /**
   * Envoie une notification via le service externe.
   *
   * @param payload description générique de la notification à envoyer
   */
  void send(SendNotificationPayload payload);
}
