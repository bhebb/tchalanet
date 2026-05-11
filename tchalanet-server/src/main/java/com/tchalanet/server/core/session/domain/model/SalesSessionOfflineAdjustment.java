package com.tchalanet.server.core.session.domain.model;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import java.time.Instant;

public record SalesSessionOfflineAdjustment(
    TenantId tenantId,
    SalesSessionId sessionId,
    TicketId ticketId,
    long amountCents,
    CurrencyCode currency,
    Instant occurredAtDevice,
    Instant recordedAt,
    String reason
) {}
