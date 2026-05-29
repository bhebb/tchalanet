package com.tchalanet.server.core.seller.internal.domain.model;

import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.seller.api.model.SellerStatus;

import java.time.Instant;

public record Seller(
    SellerId id,
    TenantId tenantId,
    UserId userId,
    String code,
    String displayName,
    SellerStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public Seller {
        if (id == null) throw new IllegalArgumentException("seller.id_required");
        if (tenantId == null) throw new IllegalArgumentException("seller.tenant_required");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("seller.display_name_required");
        if (status == null) throw new IllegalArgumentException("seller.status_required");
    }

    public boolean activeForSale() {
        return status == SellerStatus.ACTIVE;
    }

    public Seller withStatus(SellerStatus newStatus, Instant updatedAt) {
        return new Seller(id, tenantId, userId, code, displayName, newStatus, createdAt, updatedAt);
    }

    public Seller withUserId(UserId newUserId, Instant updatedAt) {
        return new Seller(id, tenantId, newUserId, code, displayName, status, createdAt, updatedAt);
    }

    public static Seller create(
        SellerId id,
        TenantId tenantId,
        UserId userId,
        String code,
        String displayName,
        SellerStatus status,
        Instant createdAt
    ) {
        return new Seller(id, tenantId, userId, code, displayName, status, createdAt, createdAt);
    }
}
