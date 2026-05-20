package com.tchalanet.server.core.session.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record GetCashierIdentityQuery(
    TenantId tenantId,
    UserId cashierId
) implements Query<CashierIdentityView> {}
