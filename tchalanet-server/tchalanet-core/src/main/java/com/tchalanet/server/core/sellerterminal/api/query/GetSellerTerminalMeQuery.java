package com.tchalanet.server.core.sellerterminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalView;

/** Resolves /tenant/terminal/me — called with the SellerTerminalId already resolved from context. */
public record GetSellerTerminalMeQuery(
    TenantId tenantId,
    SellerTerminalId sellerTerminalId
) implements Query<SellerTerminalView> {}
