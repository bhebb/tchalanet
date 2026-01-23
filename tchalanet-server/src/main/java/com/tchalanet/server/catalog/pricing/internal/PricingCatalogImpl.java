package com.tchalanet.server.catalog.pricing.internal;

import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
import com.tchalanet.server.catalog.pricing.cache.PricingCacheNames;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsJpaRepository;
import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingCatalogImpl implements PricingCatalog {

  private final PricingOddsJpaRepository repo;

  @Override
  @Cacheable(cacheNames = PricingCacheNames.ODDS, key = "#tenantId + ':' + #gameCode + ':' + #betType + ':' + #betOption")
  public BigDecimal oddsFor(TenantId tenantId, String gameCode, BetType betType, Short betOption) {
    UUID tenant = tenantId == null ? null : tenantId.value();
    Optional<PricingOddsEntity> opt = repo.findFirstByTenantIdAndGameCodeAndBetTypeAndBetOptionAndActiveIsTrueAndDeletedAtIsNull(
        tenant, gameCode, betType, betOption);
    return opt.map(PricingOddsEntity::getOdds).orElse(BigDecimal.ONE);
  }
}
