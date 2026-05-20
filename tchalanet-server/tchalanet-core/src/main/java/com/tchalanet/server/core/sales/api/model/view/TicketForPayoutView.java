package com.tchalanet.server.core.sales.api.model.view;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;

public record TicketForPayoutView(
    TicketId id,
    TenantId tenantId,
    String ticketCode,
    TicketSaleStatus status,
    DrawId drawId,
    long winAmountCents,
    String currency
) {}
