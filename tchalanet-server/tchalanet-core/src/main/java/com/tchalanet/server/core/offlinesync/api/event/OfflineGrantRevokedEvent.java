package com.tchalanet.server.core.offlinesync.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record OfflineGrantRevokedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    OfflineSalesGrantId grantId,
    UserId revokedBy,
    String reason
) implements DomainEvent {}
