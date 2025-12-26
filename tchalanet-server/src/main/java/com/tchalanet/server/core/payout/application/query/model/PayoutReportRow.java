package com.tchalanet.server.core.payout.application.query.model;

import com.tchalanet.server.common.types.id.OutletId;
import java.time.Instant;
import java.util.UUID;

public record PayoutReportRow(
    UUID payoutId,
    UUID ticketId,
    OutletId payingOutletId,
    OutletId sellingOutletId,
    long amountCents,
    String currency,
    String status,
    Instant createdAt,
    Instant approvedAt,
    Instant paidAt
) {}

