package com.tchalanet.server.core.sales.internal.infra.web.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.time.Instant;

public record TicketDetailsResponse(
    TicketId id,
    String ticketCode,
    TicketSaleStatus status,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    DrawId drawId,
    long totalAmountCents,
    long winningAmountCents,
    long paidAmountCents,
    String currency,
    Instant placedAt,
    Instant cancelledAt) {}
