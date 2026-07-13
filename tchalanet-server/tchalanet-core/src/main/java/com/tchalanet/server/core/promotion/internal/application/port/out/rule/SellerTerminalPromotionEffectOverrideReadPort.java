package com.tchalanet.server.core.promotion.internal.application.port.out.rule;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.internal.domain.model.SellerTerminalPromotionEffectOverride;

import java.util.List;

public interface SellerTerminalPromotionEffectOverrideReadPort {
    List<SellerTerminalPromotionEffectOverride> findActiveOverrides(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId
    );
}
