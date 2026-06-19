package com.tchalanet.server.core.sellerterminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;

public record GetCurrentOperationalContextQuery(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId,
    OperationalContextSource source,
    OperationalContextTrust trust,
    boolean trustedForSensitiveOperation
) implements Query<CurrentOperationalContextView> {}
