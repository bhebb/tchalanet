package com.tchalanet.server.core.terminal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record TerminalLockedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TerminalId terminalId,
    String reason,
    UserId actorUserId)
    implements DomainEvent {}
