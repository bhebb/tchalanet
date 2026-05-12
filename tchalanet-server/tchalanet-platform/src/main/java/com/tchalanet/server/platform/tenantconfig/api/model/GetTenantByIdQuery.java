package com.tchalanet.server.platform.tenantconfig.api.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;

public record GetTenantByIdQuery(TenantId tenantId) implements Query<TenantConfigView> {}
