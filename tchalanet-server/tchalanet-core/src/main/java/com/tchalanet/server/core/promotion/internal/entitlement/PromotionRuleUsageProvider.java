package com.tchalanet.server.core.promotion.internal.entitlement;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.internal.infra.persistence.repository.PromotionRuleRepository;
import com.tchalanet.server.platform.entitlement.api.UsageKeys;
import com.tchalanet.server.platform.entitlement.api.UsageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromotionRuleUsageProvider implements UsageProvider {

    private final PromotionRuleRepository repository;

    @Override
    public boolean supports(String usageKey) {
        return UsageKeys.PROMOTION_RULES_ACTIVE.equals(usageKey);
    }

    @Override
    public int currentUsage(TenantId tenantId, String usageKey) {
        if (!supports(usageKey)) {
            throw new IllegalArgumentException("Unsupported usageKey: " + usageKey);
        }

        return Math.toIntExact(repository.countByTenantIdAndDeletedAtIsNull(tenantId.value()));
    }
}
