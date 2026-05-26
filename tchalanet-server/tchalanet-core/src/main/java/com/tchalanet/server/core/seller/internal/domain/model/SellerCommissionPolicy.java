package com.tchalanet.server.core.seller.internal.domain.model;

import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerCommissionPolicyId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.seller.api.model.SellerAssignmentStatus;
import com.tchalanet.server.core.seller.api.model.SellerCommissionBase;
import com.tchalanet.server.core.seller.api.model.SellerCommissionType;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerCommissionPolicy(
    SellerCommissionPolicyId id,
    TenantId tenantId,
    SellerId sellerId,
    SellerCommissionType type,
    SellerCommissionBase base,
    BigDecimal ratePercent,
    BigDecimal fixedAmount,
    String currency,
    Instant startsAt,
    Instant endsAt,
    SellerAssignmentStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public SellerCommissionPolicy {
        if (id == null) throw new IllegalArgumentException("seller_commission_policy.id_required");
        if (tenantId == null) throw new IllegalArgumentException("seller_commission_policy.tenant_required");
        if (sellerId == null) throw new IllegalArgumentException("seller_commission_policy.seller_required");
        if (type == null) throw new IllegalArgumentException("seller_commission_policy.type_required");
        if (base == null) throw new IllegalArgumentException("seller_commission_policy.base_required");
        if (startsAt == null) throw new IllegalArgumentException("seller_commission_policy.starts_at_required");
        if (status == null) throw new IllegalArgumentException("seller_commission_policy.status_required");
    }

    public boolean isActive() {
        return status == SellerAssignmentStatus.ACTIVE && endsAt == null;
    }

    public SellerCommissionPolicy end(Instant endedAt) {
        return new SellerCommissionPolicy(id, tenantId, sellerId, type, base, ratePercent, fixedAmount,
            currency, startsAt, endedAt, SellerAssignmentStatus.ENDED, createdAt, endedAt);
    }
}
