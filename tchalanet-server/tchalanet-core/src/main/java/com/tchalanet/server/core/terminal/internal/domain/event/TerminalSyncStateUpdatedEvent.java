package com.tchalanet.server.core.terminal.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSyncState;
import java.time.Instant;

public record TerminalSyncStateUpdatedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TerminalId terminalId,
    TerminalSyncState fromState,
    TerminalSyncState toState,
    UserId actorUserId)
    implements DomainEvent {}
