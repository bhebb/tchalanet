package com.tchalanet.server.features.notifications.domain.ports.out;

import java.util.UUID;

/** Port de sortie pour l'envoi de notifications (SMS, email, WhatsApp, etc.). */
public interface NotificationPort {

  void sendVendorMessage(UUID tenantId, UUID userId, String message);

  void sendAdminAlert(UUID tenantId, String subject, String message);

  void sendTicketLink(UUID tenantId, String channel, String destination, String ticketPublicCode);
}
