package com.tchalanet.server.core.payout.infra.web.model;



import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.payout.domain.model.PayoutStatus;
import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;

public record PayoutSearchRequest(
    PayoutStatus status,
    TicketId ticketId,
    OutletId outletId,
    SalesSessionId sessionId,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
) {}

