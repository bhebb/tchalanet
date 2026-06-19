package com.tchalanet.server.core.sales.api.model.view;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;

import java.time.Instant;

public record TicketDetailsView(
    TicketId id,
    TenantId tenantId,
    String ticketCode,
    TicketSaleStatus status,
    DrawId drawId,
    SellerTerminalId sellerTerminalId,
    long totalAmountCents,
    String currency,
    Instant placedAt,
    Instant cancelledAt
) {}
