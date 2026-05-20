package com.tchalanet.server.features.cashier.operationalcontext;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;

public record SellerOperationalContextView(
    TenantId tenantId,
    UserId actorUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    Locale locale,
    ZoneId timezone,
    Currency currency,
    Set<String> permissions
) {}
