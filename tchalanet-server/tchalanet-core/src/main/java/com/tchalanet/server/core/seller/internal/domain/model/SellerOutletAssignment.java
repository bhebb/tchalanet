package com.tchalanet.server.core.seller.internal.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.seller.api.model.SellerAssignmentStatus;

import java.time.Instant;

public record SellerOutletAssignment(
    SellerOutletAssignmentId id,
    TenantId tenantId,
    SellerId sellerId,
    OutletId outletId,
    Instant startsAt,
    Instant endsAt,
    SellerAssignmentStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public SellerOutletAssignment {
        if (id == null) throw new IllegalArgumentException("seller_assignment.id_required");
        if (tenantId == null) throw new IllegalArgumentException("seller_assignment.tenant_required");
        if (sellerId == null) throw new IllegalArgumentException("seller_assignment.seller_required");
        if (outletId == null) throw new IllegalArgumentException("seller_assignment.outlet_required");
        if (startsAt == null) throw new IllegalArgumentException("seller_assignment.starts_at_required");
        if (status == null) throw new IllegalArgumentException("seller_assignment.status_required");
    }

    public boolean isActive() {
        return status == SellerAssignmentStatus.ACTIVE && endsAt == null;
    }

    public SellerOutletAssignment end(Instant endedAt) {
        return new SellerOutletAssignment(id, tenantId, sellerId, outletId, startsAt, endedAt,
            SellerAssignmentStatus.ENDED, createdAt, endedAt);
    }
}
