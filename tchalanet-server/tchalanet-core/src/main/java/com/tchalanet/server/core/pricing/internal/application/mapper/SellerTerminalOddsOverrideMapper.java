package com.tchalanet.server.core.pricing.internal.application.mapper;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalOddsOverrideView;
import com.tchalanet.server.core.pricing.internal.domain.SellerTerminalOddsOverride;
import org.springframework.stereotype.Component;

@Component
public class SellerTerminalOddsOverrideMapper {

    public SellerTerminalOddsOverrideView toView(SellerTerminalOddsOverride o) {
        return new SellerTerminalOddsOverrideView(
            o.id(), o.tenantId(), o.sellerTerminalId(),
            o.gameCode(), o.betType(), o.betOption(),
            o.odds(), o.active(),
            o.effectiveFrom(), o.effectiveTo(),
            o.reason(), o.createdAt(), o.updatedAt());
    }

    public SellerTerminalOddsOverride fromEntity(
        SellerTerminalOddsOverrideId id,
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        String betType,
        Short betOption,
        java.math.BigDecimal odds,
        boolean active,
        java.time.Instant effectiveFrom,
        java.time.Instant effectiveTo,
        String reason,
        java.time.Instant createdAt,
        com.tchalanet.server.common.types.id.UserId createdBy,
        java.time.Instant updatedAt,
        com.tchalanet.server.common.types.id.UserId updatedBy,
        java.time.Instant deletedAt
    ) {
        return new SellerTerminalOddsOverride(
            id, tenantId, sellerTerminalId,
            gameCode, betType, betOption,
            odds, active,
            effectiveFrom, effectiveTo, reason,
            createdAt, createdBy, updatedAt, updatedBy, deletedAt);
    }
}
