package com.tchalanet.server.platform.accesscontrol.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;

public record ListRolesRequest(TenantId tenantId) {}
