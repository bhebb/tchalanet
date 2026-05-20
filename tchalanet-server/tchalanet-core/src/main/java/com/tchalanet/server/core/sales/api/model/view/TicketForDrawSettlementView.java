package com.tchalanet.server.core.sales.api.model.view;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;

public record TicketForDrawSettlementView(
    TicketId id,
    DrawId drawId,
    TicketSaleStatus status,
    long totalAmountCents,
    String currency
) {}
