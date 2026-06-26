package com.tchalanet.server.platform.notification.api.model.view;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.notification.api.model.NotificationTarget;

public record NotificationRecipientContact(
    TenantId tenantId,
    NotificationTarget target,
    UserId userId,
    String email,
    String phone) {}
