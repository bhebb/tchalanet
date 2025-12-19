package com.tchalanet.server.core.payout.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayoutRegisteredEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    UUID payoutId,
    UUID ticketId,
    BigDecimal amount) implements DomainEvent {
}

