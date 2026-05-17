package com.tchalanet.server.core.offlinesync.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;

public record OfflineGrantIssuedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSalesGrantId grantId,
    TerminalId terminalId,
    OutletId outletId,
    Instant expiresAt
) implements DomainEvent {}
