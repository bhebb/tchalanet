package com.tchalanet.server.core.notification.domain.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.annotation.Nullable;

/**
 * Modèle flexible de destinataire de notification supportant différents canaux.
 *
 * - SLACK: utilise channelKey
 * - EMAIL: utilise to (email)
 * - SMS: utilise to (téléphone)
 * - WEB: utilise tenantId/userId
 */
public record NotificationRecipient(
    NotificationChannel channel,
    @Nullable String to,
    @Nullable String channelKey,
    @Nullable TenantId tenantId,
    @Nullable UserId userId
) {
    public NotificationRecipient {
        if (channel == null) {
            throw new IllegalArgumentException("channel is required");
        }
    }
}

