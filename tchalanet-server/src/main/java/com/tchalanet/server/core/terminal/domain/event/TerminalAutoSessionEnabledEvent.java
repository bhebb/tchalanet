package com.tchalanet.server.core.terminal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record TerminalAutoSessionEnabledEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TerminalId terminalId,
    UserId userId,
    UserId actorUserId)
    implements DomainEvent {}
