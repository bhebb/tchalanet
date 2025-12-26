package com.tchalanet.server.core.session.domain.event;
import com.tchalanet.server.common.types.id.SessionId;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.util.UUID;

public record SessionOpenedEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    SessionId sessionId,
    OutletId outletId,
    TerminalId terminalId,
    UserId cashierId,
    long openingFloatCents) implements DomainEvent {}
