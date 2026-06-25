package com.tchalanet.server.core.analytics.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView;
import java.time.LocalDate;

/**
 * Returns tenant-admin financial drilldowns for commissions, charges and promotions.
 *
 * <p>Executed via {@code QueryBus}; features must not read analytics tables directly.
 */
public record GetTenantFinancialBreakdownQuery(
    TenantId tenantId,
    LocalDate from,
    LocalDate to,
    int drawLimit,
    int sellerTerminalLimit
) implements Query<TenantFinancialBreakdownView> {}
