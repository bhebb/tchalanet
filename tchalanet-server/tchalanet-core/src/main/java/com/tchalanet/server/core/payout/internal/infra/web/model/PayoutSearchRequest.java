package com.tchalanet.server.core.payout.internal.infra.web.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;

public record PayoutSearchRequest(
    PayoutClaimStatus status,
    TicketId ticketId,
    OutletId outletId,
    SalesSessionId sessionId,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
) {}
