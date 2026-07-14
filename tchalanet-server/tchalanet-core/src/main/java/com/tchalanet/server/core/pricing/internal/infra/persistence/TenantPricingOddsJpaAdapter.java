package com.tchalanet.server.core.pricing.internal.infra.persistence;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import com.tchalanet.server.core.pricing.internal.infra.cache.PricingCacheNames;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantPricingOddsJpaAdapter
    implements TenantPricingOddsReaderPort, TenantPricingOddsWriterPort {

  private final TenantPricingOddsJpaRepository repo;

  @Override
  @Cacheable(
      cacheNames = PricingCacheNames.TENANT_ODDS_LIST,
      key =
          "#tenantId.value().toString() + ':' + (#gameCode == null ? '' : #gameCode.trim().toUpperCase())")
  public List<TenantPricingOdds> findActiveByTenant(TenantId tenantId, String gameCode) {
    var normalizedGameCode =
        gameCode == null || gameCode.isBlank()
            ? null
            : TenantPricingOdds.normalizeGameCode(gameCode);
    return repo.findActiveByTenant(tenantId.value(), normalizedGameCode).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  @Cacheable(
      cacheNames = PricingCacheNames.TENANT_ODDS_BY_VARIANT,
      key = "#tenantId.value().toString() + ':' + #gameCode + ':' + #pricingVariantCode.name()")
  public Optional<TenantPricingOdds> findByNaturalKey(
      TenantId tenantId, String gameCode, PricingVariantCode pricingVariantCode) {
    return repo.findByNaturalKey(tenantId.value(), gameCode, pricingVariantCode.name())
        .map(this::toDomain);
  }

  @Override
  @CacheEvict(
      cacheNames = {PricingCacheNames.TENANT_ODDS_LIST, PricingCacheNames.TENANT_ODDS_BY_VARIANT},
      allEntries = true)
  public TenantPricingOdds save(TenantPricingOdds odds) {
    var entity =
        repo.findByNaturalKey(
                odds.tenantId().value(), odds.gameCode(), odds.pricingVariantCode().name())
            .orElseGet(TenantPricingOddsJpaEntity::new);
    toEntity(odds, entity);
    return toDomain(repo.save(entity));
  }

  private TenantPricingOdds toDomain(TenantPricingOddsJpaEntity e) {
    return new TenantPricingOdds(
        TenantId.of(e.getTenantId()),
        e.getGameCode(),
        PricingVariantCode.valueOf(e.getPricingVariantCode()),
        e.getBetType(),
        e.getBetOption(),
        e.getOdds(),
        e.getPayoutRuleType(),
        e.getFixedAmount(),
        e.isActive(),
        e.getDeletedAt());
  }

  private void toEntity(TenantPricingOdds odds, TenantPricingOddsJpaEntity e) {
    e.setTenantId(odds.tenantId().value());
    e.setGameCode(odds.gameCode());
    e.setPricingVariantCode(odds.pricingVariantCode().name());
    e.setBetType(odds.betType());
    e.setBetOption(odds.betOption());
    e.setOdds(odds.odds());
    e.setPayoutRuleType(odds.payoutRuleType());
    e.setFixedAmount(odds.fixedAmount());
    e.setActive(odds.active());
    e.setDeletedAt(odds.deletedAt());
  }
}
