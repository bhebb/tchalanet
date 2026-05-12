package com.tchalanet.server.core.terminal.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.domain.model.TerminalKind;
import java.time.Instant;

public record TerminalRegisteredEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    TerminalId terminalId,
    OutletId outletId,
    TerminalKind kind,
    String label,
    UserId actorUserId)
    implements DomainEvent {}
