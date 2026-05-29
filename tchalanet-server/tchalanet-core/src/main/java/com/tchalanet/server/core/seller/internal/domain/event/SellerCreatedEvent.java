package com.tchalanet.server.core.seller.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

public record SellerCreatedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    SellerId sellerId,
    UserId userId,
    String displayName
) implements DomainEvent {}
