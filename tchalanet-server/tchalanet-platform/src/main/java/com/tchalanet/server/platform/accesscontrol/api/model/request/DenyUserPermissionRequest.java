package com.tchalanet.server.platform.accesscontrol.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record DenyUserPermissionRequest(TenantId tenantId, UserId userId, String permissionCode, String reason, UserId deniedBy) {}
