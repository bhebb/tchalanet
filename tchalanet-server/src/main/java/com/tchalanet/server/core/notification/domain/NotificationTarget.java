package com.tchalanet.server.core.notification.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Cible d'une notification (tenant, utilisateur, destinataire brut).
 */
public record NotificationTarget(
    UUID tenantId,
    UUID userId,
    String recipient) {

    public NotificationTarget {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(recipient, "recipient is required");
    }

}

