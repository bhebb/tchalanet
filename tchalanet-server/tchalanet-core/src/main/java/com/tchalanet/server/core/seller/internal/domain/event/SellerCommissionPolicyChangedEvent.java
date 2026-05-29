package com.tchalanet.server.core.seller.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerCommissionPolicyId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.seller.api.model.SellerCommissionType;

import java.time.Instant;

public record SellerCommissionPolicyChangedEvent(
    EventId eventId,
    Instant occurredAt,
    TenantId tenantId,
    SellerId sellerId,
    SellerCommissionPolicyId policyId,
    SellerCommissionType type
) implements DomainEvent {}
