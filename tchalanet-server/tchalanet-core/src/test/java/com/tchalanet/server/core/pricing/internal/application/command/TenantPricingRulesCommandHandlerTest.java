package com.tchalanet.server.core.pricing.internal.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.command.DeleteTenantPricingRuleCommand;
import com.tchalanet.server.core.pricing.api.command.EnsureDefaultHaitiLotteryPricingRulesCommand;
import com.tchalanet.server.core.pricing.api.command.UpsertTenantPricingRuleCommand;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.internal.application.mapper.TenantPricingOddsMapper;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantPricingRulesCommandHandlerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));

  private final InMemoryTenantPricingOddsStore store = new InMemoryTenantPricingOddsStore();
  private final TenantPricingOddsMapper mapper = new TenantPricingOddsMapper();

  @Test
  void upsertCreatesTenantRuleByPricingVariant() {
    var handler = new UpsertTenantPricingRuleCommandHandler(store, store, mapper);

    var result =
        handler.handle(
            new UpsertTenantPricingRuleCommand(
                TENANT_ID,
                "ht_loto4",
                PricingVariantCode.LOTTO4_BOX_4_WAY,
                "lotto4_pattern",
                (short) 2,
                new BigDecimal("1200.0000"),
                null));

    assertThat(result.gameCode()).isEqualTo("HT_LOTO4");
    assertThat(result.pricingVariantCode()).isEqualTo(PricingVariantCode.LOTTO4_BOX_4_WAY);
    assertThat(result.betType()).isEqualTo("LOTTO4_PATTERN");
    assertThat(result.odds()).isEqualByComparingTo("1200.0000");
    assertThat(result.active()).isTrue();
  }

  @Test
  void upsertAllowsMaryajGratisOnlyAsFixedAmount() {
    var handler = new UpsertTenantPricingRuleCommandHandler(store, store, mapper);

    var result =
        handler.handle(
            new UpsertTenantPricingRuleCommand(
                TENANT_ID,
                "ht_maryaj_gratis",
                PricingVariantCode.MARRIAGE_EXACT_ORDER,
                "marriage_2d2d",
                (short) 1,
                null,
                PayoutRuleType.FIXED_AMOUNT,
                new BigDecimal("2000.0000"),
                null));

    assertThat(result.gameCode()).isEqualTo("HT_MARYAJ_GRATIS");
    assertThat(result.payoutRuleType()).isEqualTo(PayoutRuleType.FIXED_AMOUNT);
    assertThat(result.fixedAmount()).isEqualByComparingTo("2000.0000");
    assertThat(result.odds()).isEqualByComparingTo("1.0000");
  }

  @Test
  void upsertRejectsMaryajGratisMultiplier() {
    var handler = new UpsertTenantPricingRuleCommandHandler(store, store, mapper);

    assertThatThrownBy(
            () ->
                handler.handle(
                    new UpsertTenantPricingRuleCommand(
                        TENANT_ID,
                        "HT_MARYAJ_GRATIS",
                        PricingVariantCode.MARRIAGE_EXACT_ORDER,
                        "MARRIAGE_2D2D",
                        (short) 1,
                        new BigDecimal("50.0000"),
                        PayoutRuleType.STAKE_MULTIPLIER,
                        null,
                        null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires payoutRuleType FIXED_AMOUNT");
  }

  @Test
  void upsertRejectsPaidGameFixedAmount() {
    var handler = new UpsertTenantPricingRuleCommandHandler(store, store, mapper);

    assertThatThrownBy(
            () ->
                handler.handle(
                    new UpsertTenantPricingRuleCommand(
                        TENANT_ID,
                        "HT_LOTO4",
                        PricingVariantCode.LOTTO4_STRAIGHT,
                        "LOTTO4_PATTERN",
                        (short) 1,
                        null,
                        PayoutRuleType.FIXED_AMOUNT,
                        new BigDecimal("5000.0000"),
                        null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires payoutRuleType STAKE_MULTIPLIER");
  }

  @Test
  void upsertReactivatesDeletedTenantRuleWithoutCreatingDuplicateNaturalKey() {
    var handler = new UpsertTenantPricingRuleCommandHandler(store, store, mapper);
    store.saved.add(
        new TenantPricingOdds(
            TENANT_ID,
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BOX_4_WAY,
            "LOTTO4_PATTERN",
            (short) 2,
            new BigDecimal("1200.0000"),
            false,
            null));

    handler.handle(
        new UpsertTenantPricingRuleCommand(
            TENANT_ID,
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BOX_4_WAY,
            "LOTTO4_PATTERN",
            (short) 2,
            new BigDecimal("1300.0000"),
            null));

    assertThat(store.saved).hasSize(1);
    assertThat(store.saved.getFirst().active()).isTrue();
    assertThat(store.saved.getFirst().odds()).isEqualByComparingTo("1300.0000");
  }

  @Test
  void deleteDisablesTenantRuleButKeepsNaturalKeyReusable() {
    var handler = new DeleteTenantPricingRuleCommandHandler(store, store);
    store.saved.add(
        new TenantPricingOdds(
            TENANT_ID,
            "HT_LOTO3",
            PricingVariantCode.LOTTO3_BOX_6_WAY,
            "LOTTO3_3D",
            (short) 2,
            new BigDecimal("500.0000"),
            true,
            null));

    handler.handle(
        new DeleteTenantPricingRuleCommand(
            TENANT_ID, "HT_LOTO3", PricingVariantCode.LOTTO3_BOX_6_WAY, null));

    assertThat(store.saved).hasSize(1);
    assertThat(store.saved.getFirst().active()).isFalse();
    assertThat(store.saved.getFirst().deletedAt()).isNull();
  }

  @Test
  void deleteMissingTenantRuleIsIdempotent() {
    var handler = new DeleteTenantPricingRuleCommandHandler(store, store);

    handler.handle(
        new DeleteTenantPricingRuleCommand(
            TENANT_ID, "HT_LOTO3", PricingVariantCode.LOTTO3_BOX_6_WAY, null));

    assertThat(store.saved).isEmpty();
  }

  @Test
  void ensureDefaultHaitiLotteryPricingRulesSeedsVariantKeyedTenantDefaultsIdempotently() {
    var handler = new EnsureDefaultHaitiLotteryPricingRulesCommandHandler(store, store);

    handler.handle(new EnsureDefaultHaitiLotteryPricingRulesCommand(TENANT_ID));
    handler.handle(new EnsureDefaultHaitiLotteryPricingRulesCommand(TENANT_ID));

    assertThat(store.saved).hasSize(20);
    assertThat(store.findByNaturalKey(TENANT_ID, "HT_LOTO4", PricingVariantCode.LOTTO4_BOX_4_WAY))
        .get()
        .extracting(TenantPricingOdds::odds)
        .satisfies(odds -> assertThat(odds).isEqualByComparingTo("1200.0000"));
    assertThat(store.findByNaturalKey(TENANT_ID, "HT_LOTO4", PricingVariantCode.LOTTO4_BOX_24_WAY))
        .get()
        .extracting(TenantPricingOdds::odds)
        .satisfies(odds -> assertThat(odds).isEqualByComparingTo("200.0000"));
    assertThat(
            store.findByNaturalKey(
                TENANT_ID, "HT_MARYAJ_GRATIS", PricingVariantCode.MARRIAGE_EXACT_ORDER))
        .get()
        .satisfies(
            rule -> {
              assertThat(rule.payoutRuleType()).isEqualTo(PayoutRuleType.FIXED_AMOUNT);
              assertThat(rule.fixedAmount()).isEqualByComparingTo("500.0000");
              assertThat(rule.odds()).isEqualByComparingTo("1.0000");
            });
  }

  private static final class InMemoryTenantPricingOddsStore
      implements TenantPricingOddsReaderPort, TenantPricingOddsWriterPort {

    private final List<TenantPricingOdds> saved = new ArrayList<>();

    @Override
    public List<TenantPricingOdds> findActiveByTenant(TenantId tenantId, String gameCode) {
      return saved.stream()
          .filter(TenantPricingOdds::active)
          .filter(odds -> odds.tenantId().equals(tenantId))
          .filter(odds -> gameCode == null || odds.gameCode().equals(gameCode))
          .toList();
    }

    @Override
    public Optional<TenantPricingOdds> findByNaturalKey(
        TenantId tenantId, String gameCode, PricingVariantCode pricingVariantCode) {
      return saved.stream()
          .filter(odds -> odds.tenantId().equals(tenantId))
          .filter(odds -> odds.gameCode().equals(gameCode))
          .filter(odds -> odds.pricingVariantCode() == pricingVariantCode)
          .findFirst();
    }

    @Override
    public TenantPricingOdds save(TenantPricingOdds odds) {
      findByNaturalKey(odds.tenantId(), odds.gameCode(), odds.pricingVariantCode())
          .ifPresent(saved::remove);
      saved.add(odds);
      return odds;
    }
  }
}
