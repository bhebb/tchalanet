package com.tchalanet.server.core.draw.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;

import java.time.Instant;
import java.util.UUID;

public record DrawSettledEvent(
    UUID eventId,
    Instant occurredAt,
    TenantId tenantId,
    UUID drawId,
    String gameCode,
    Instant scheduledAt,
    String channelCode) implements DomainEvent {}

