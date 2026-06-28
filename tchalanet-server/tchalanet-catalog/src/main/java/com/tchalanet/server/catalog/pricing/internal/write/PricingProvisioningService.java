package com.tchalanet.server.catalog.pricing.internal.write;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.pricing.api.PricingProvisioningApi;
import com.tchalanet.server.catalog.pricing.internal.cache.PricingCacheNames;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsJpaRepository;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingProvisioningService implements PricingProvisioningApi {

  private final PricingOddsJpaRepository repository;

  @Override
  @Transactional
  @CacheEvict(cacheNames = {PricingCacheNames.ODDS}, allEntries = true)
  public void ensureDefaultHaitiLotteryOdds(TenantId tenantId) {
    if (tenantId == null) {
      throw new IllegalArgumentException("tenantId is required");
    }
    for (DefaultPricingOdds row : defaultHaitiLotteryOdds()) {
      ensureOdds(tenantId, row);
    }
  }

  private void ensureOdds(TenantId tenantId, DefaultPricingOdds row) {
    var existing = repository.findFirstByTenantIdAndGameCodeAndBetTypeAndBetOptionAndDeletedAtIsNull(
        tenantId.value(),
        row.gameCode(),
        row.betType(),
        row.betOption());
    if (existing.isPresent()) {
      var entity = existing.get();
      entity.setOdds(row.odds());
      entity.setActive(true);
      repository.save(entity);
      return;
    }

    var entity = new PricingOddsEntity();
    entity.setTenantId(tenantId.value());
    entity.setGameCode(row.gameCode());
    entity.setBetType(row.betType());
    entity.setBetOption(row.betOption());
    entity.setOdds(row.odds());
    entity.setActive(true);
    repository.save(entity);
  }

  private static List<DefaultPricingOdds> defaultHaitiLotteryOdds() {
    return List.of(
        new DefaultPricingOdds("HT_BOLET", BetType.MATCH_1_2D, null, new BigDecimal("50.0000")),
        new DefaultPricingOdds("HT_BOLET", BetType.MATCH_2_2D, null, new BigDecimal("20.0000")),
        new DefaultPricingOdds("HT_BOLET", BetType.MATCH_3_2D, null, new BigDecimal("10.0000")),
        new DefaultPricingOdds("HT_MARYAJ", BetType.MARRIAGE_2D2D, null, new BigDecimal("1000.0000")),
        new DefaultPricingOdds("HT_MARYAJ_GRATUIT", BetType.MARRIAGE_2D2D, null, new BigDecimal("10.0000")),
        new DefaultPricingOdds("HT_LOTO3", BetType.LOTTO3_3D, null, new BigDecimal("500.0000")),
        new DefaultPricingOdds("HT_LOTO4", BetType.LOTTO4_PATTERN, (short) 1, new BigDecimal("5000.0000")),
        new DefaultPricingOdds("HT_LOTO4", BetType.LOTTO4_PATTERN, (short) 2, new BigDecimal("5000.0000")),
        new DefaultPricingOdds("HT_LOTO4", BetType.LOTTO4_PATTERN, (short) 3, new BigDecimal("5000.0000")),
        new DefaultPricingOdds("HT_LOTO4", BetType.LOTTO4_PATTERN, (short) 4, new BigDecimal("5000.0000")),
        new DefaultPricingOdds("HT_LOTO5", BetType.LOTTO5_PATTERN, (short) 1, new BigDecimal("25000.0000")),
        new DefaultPricingOdds("HT_LOTO5", BetType.LOTTO5_PATTERN, (short) 2, new BigDecimal("25000.0000")),
        new DefaultPricingOdds("HT_LOTO5", BetType.LOTTO5_PATTERN, (short) 3, new BigDecimal("25000.0000")));
  }

  private record DefaultPricingOdds(
      String gameCode,
      BetType betType,
      Short betOption,
      BigDecimal odds) {}
}
