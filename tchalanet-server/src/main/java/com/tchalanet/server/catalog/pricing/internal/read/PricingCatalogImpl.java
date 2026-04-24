package com.tchalanet.server.catalog.pricing.internal.read;

import com.tchalanet.server.catalog.pricing.api.PricingCatalog;
import com.tchalanet.server.catalog.pricing.internal.cache.PricingCacheNames;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsJpaRepository;
import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.util.Optional;
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
    // RLS will scope results to the current tenant; do not pass tenantId in SQL from read-side
    Optional<PricingOddsEntity> opt = repo.findFirstByGameCodeAndBetTypeAndBetOptionAndActiveIsTrue(
        gameCode, betType, betOption);
    return opt.map(PricingOddsEntity::getOdds).orElse(BigDecimal.ONE);
  }

  @Override
  public com.tchalanet.server.catalog.pricing.api.model.PricingStatsView stats() {
    long total = repo.count();
    long active = repo.findAll().stream().filter(e -> e.isActive()).count();
    return new com.tchalanet.server.catalog.pricing.api.model.PricingStatsView((int) total, (int) active);
  }
}
