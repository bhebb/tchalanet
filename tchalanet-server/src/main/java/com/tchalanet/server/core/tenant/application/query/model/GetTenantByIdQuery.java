package com.tchalanet.server.core.tenant.application.query.model;

import com.tchalanet.server.common.bus.Query;

import java.util.UUID;

public record GetTenantByIdQuery(UUID tenantId) implements Query<UUID> {
}

