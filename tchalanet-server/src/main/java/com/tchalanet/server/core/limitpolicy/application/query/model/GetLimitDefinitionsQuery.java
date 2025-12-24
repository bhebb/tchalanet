package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.common.bus.Query;

import java.util.UUID;

public record GetLimitDefinitionsQuery(UUID tenantId) implements Query<GetLimitDefinitionsResult> {
}
