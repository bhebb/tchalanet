package com.tchalanet.server.platform.tenantconfig.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;

public record GetTenantByIdRequest(TenantId tenantId) {}
