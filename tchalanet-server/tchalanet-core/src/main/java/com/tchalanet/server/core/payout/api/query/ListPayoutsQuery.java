package com.tchalanet.server.core.payout.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
import java.time.Instant;
import org.springframework.data.domain.Pageable;

public record ListPayoutsQuery(
    PayoutClaimStatus status,
    TicketId ticketId,
    OutletId outletId,
    SalesSessionId sessionId,
    Instant from,
    Instant to,
    Pageable pageable
) implements Query<TchPage<PayoutRow>> {}
