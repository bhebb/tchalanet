package com.tchalanet.server.core.notification.domain;

import java.util.Objects;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

/**
 * Cible d'une notification (tenant, utilisateur, destinataire brut).
 */
public record NotificationTarget(
    TenantId tenantId,
    UserId userId,
    String recipient) {

    public NotificationTarget {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(recipient, "recipient is required");
    }

}
