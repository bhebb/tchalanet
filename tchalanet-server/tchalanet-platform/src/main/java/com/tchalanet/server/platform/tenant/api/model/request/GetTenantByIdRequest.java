package com.tchalanet.server.platform.tenant.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;

public record GetTenantByIdRequest(TenantId tenantId) {}
