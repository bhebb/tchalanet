package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;

public record GetLimitDefinitionsQuery(TenantId tenantId)
    implements Query<GetLimitDefinitionsResult> {}
