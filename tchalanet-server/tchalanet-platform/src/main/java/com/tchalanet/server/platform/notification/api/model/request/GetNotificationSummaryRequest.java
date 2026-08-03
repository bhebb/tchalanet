package com.tchalanet.server.platform.notification.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record GetNotificationSummaryRequest(TenantId tenantId, UserId userId, String roleCode) {}
