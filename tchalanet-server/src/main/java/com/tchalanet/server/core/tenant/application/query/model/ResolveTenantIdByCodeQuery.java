package com.tchalanet.server.core.tenant.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Query;

import java.util.UUID;

public record ResolveTenantIdByCodeQuery(String code) implements Query<UUID> {
}

