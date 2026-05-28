package com.tchalanet.server.core.session.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

/**
 * @deprecated Use {@link com.tchalanet.server.core.session.api.event.SalesSessionClosedEvent}
 *             (public API). This internal copy will be removed in a future release.
 */
@Deprecated(since = "analytics-v1", forRemoval = true)
public record SalesSessionClosedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    SalesSessionId sessionId, OutletId outletId, TerminalId terminalId, UserId actorId, String reason) implements DomainEvent {}
