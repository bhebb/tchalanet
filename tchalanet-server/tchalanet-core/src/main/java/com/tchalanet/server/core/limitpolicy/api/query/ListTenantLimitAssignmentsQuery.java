package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;

public record ListTenantLimitAssignmentsQuery(TenantId tenantId)
    implements Query<ListTenantLimitAssignmentsView> {}
