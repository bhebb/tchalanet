package com.tchalanet.server.core.sellerterminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;

public record GetSellerTerminalSettingsQuery(TenantId tenantId, SellerTerminalId sellerTerminalId)
    implements Query<SellerTerminalSettingsView> {}
