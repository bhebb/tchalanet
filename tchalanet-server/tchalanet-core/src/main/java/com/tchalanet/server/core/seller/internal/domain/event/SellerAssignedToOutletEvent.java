package com.tchalanet.server.core.seller.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;

public record SellerAssignedToOutletEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    SellerId sellerId,
    OutletId outletId,
    SellerOutletAssignmentId assignmentId
) implements DomainEvent {}
