package com.tchalanet.server.core.tenant.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;

public record GetTenantByIdQuery(TenantId tenantId) implements Query<UUID> {}
