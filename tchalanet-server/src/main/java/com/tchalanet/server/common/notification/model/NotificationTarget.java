package com.tchalanet.server.common.notification.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

/** Cible d'une notification (tenant, utilisateur, destinataire brut). */
public record NotificationTarget(TenantId tenantId, UserId userId, String recipient) {
  // All fields are nullable to support technical/batch notifications
}
