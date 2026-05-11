package com.tchalanet.server.core.session.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record SalesSessionClosedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    SalesSessionId sessionId, OutletId outletId, TerminalId terminalId, UserId actorId, String reason) implements DomainEvent {}
