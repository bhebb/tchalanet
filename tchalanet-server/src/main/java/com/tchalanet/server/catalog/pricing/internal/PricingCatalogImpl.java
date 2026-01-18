package com.tchalanet.server.catalog.pricing.internal;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
class PricingCatalogImpl implements PricingCatalog {

    private final PricingOddsJpaRepository repo;

    @Override
    @Cacheable(
        cacheNames = "pricing.odds",
        key =
            "T(org.springframework.cache.interceptor.SimpleKey).of(#tenantId.uuid(), #gameCode, #betType, #betOption)")
    public BigDecimal oddsFor(TenantId tenantId, String gameCode, BetType betType, Short betOption) {

        return repo
            .findFirstByTenantIdAndGameCodeAndBetTypeAndBetOptionAndActiveIsTrueAndDeletedAtIsNull(
                tenantId.uuid(), gameCode, betType, betOption)
            .map(PricingOddsEntity::getOdds)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No active odds for tenant="
                            + tenantId
                            + " gameCode="
                            + gameCode
                            + " betType="
                            + betType
                            + " betOption="
                            + betOption));
    }
}
