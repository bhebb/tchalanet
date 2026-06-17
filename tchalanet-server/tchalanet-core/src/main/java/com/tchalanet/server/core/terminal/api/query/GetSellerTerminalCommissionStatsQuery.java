package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalCommissionStatsView;

import java.math.BigDecimal;

/**
 * Returns aggregate commission stats for all non-deleted seller_terminals of a tenant.
 * {@code tenantDefaultRate} is used to partition count into "at default" vs "custom".
 * Pass null when the tenant has no default configured.
 */
public record GetSellerTerminalCommissionStatsQuery(
    TenantId tenantId,
    BigDecimal tenantDefaultRate
) implements Query<SellerTerminalCommissionStatsView> {}
