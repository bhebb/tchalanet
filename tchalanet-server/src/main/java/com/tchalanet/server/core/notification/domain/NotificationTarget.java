package com.tchalanet.server.core.notification.domain;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Objects;

/** Cible d'une notification (tenant, utilisateur, destinataire brut). */
public record NotificationTarget(TenantId tenantId, UserId userId, String recipient) {

  public NotificationTarget {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(recipient, "recipient is required");
  }
}
