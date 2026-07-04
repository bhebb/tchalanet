package com.tchalanet.server.platform.accesscontrol.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record StartSupportAccessSessionRequest(
    UserId superAdminUserId,
    TenantId tenantId,
    String tenantCode,
    String tenantName,
    String reason,
    SupportAccessMode mode
) {}
