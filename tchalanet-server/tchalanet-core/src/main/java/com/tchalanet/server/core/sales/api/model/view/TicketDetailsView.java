package com.tchalanet.server.core.sales.api.model.view;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;

import java.time.Instant;

public record TicketDetailsView(
    TicketId id,
    TenantId tenantId,
    String ticketCode,
    TicketSaleStatus status,
    DrawId drawId,
    SalesSessionId sessionId,
    OutletId outletId,
    TerminalId terminalId,
    UserId soldBy,
    OfflineSaleSubmissionId offlineSubmissionId,
    long totalAmountCents,
    String currency,
    Instant placedAt,
    Instant cancelledAt
) {}
