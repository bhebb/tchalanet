package com.tchalanet.server.core.notification.application;

import com.tchalanet.server.common.types.enums.NotificationChannel;
import com.tchalanet.server.common.types.enums.NotificationType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.annotation.Nullable;
import java.util.Map;

/** Commande d'application pour demander l'envoi d'une notification. */
public record SendNotificationCommand(
    TenantId tenantId,
    @Nullable UserId userId,
    String recipient,
    NotificationType type,
    NotificationChannel channel,
    String locale,
    Map<String, Object> data) {}
