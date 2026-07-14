package com.tchalanet.server.core.pricing.internal.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.command.UpsertSellerTerminalPricingRuleOverrideCommand;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideWriterPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.domain.SellerTerminalOddsOverride;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SellerTerminalPricingRuleOverrideCommandHandlerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final SellerTerminalId SELLER_TERMINAL_ID =
      SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));

  private final InMemorySellerTerminalOverrideStore store =
      new InMemorySellerTerminalOverrideStore();
  private final InMemoryTenantPricingStore tenantPricingStore = new InMemoryTenantPricingStore();
  private final UpsertSellerTerminalPricingRuleOverrideCommandHandler handler =
      new UpsertSellerTerminalPricingRuleOverrideCommandHandler(
          store,
          store,
          tenantPricingStore,
          () -> UUID.fromString("30000000-0000-0000-0000-000000000001"));

  @Test
  void upsertAllowsMaryajGratisFixedAmountOverride() {
    tenantPricingStore.saved.add(
        tenantDefault(
            "HT_MARYAJ_GRATIS",
            PricingVariantCode.MARRIAGE_REVERSE_ALLOWED,
            PayoutRuleType.FIXED_AMOUNT,
            BigDecimal.ONE,
            new BigDecimal("1000.0000")));

    var result =
        handler.handle(
            new UpsertSellerTerminalPricingRuleOverrideCommand(
                TENANT_ID,
                SELLER_TERMINAL_ID,
                "ht_maryaj_gratis",
                PricingVariantCode.MARRIAGE_REVERSE_ALLOWED,
                "marriage_2d2d",
                (short) 2,
                null,
                PayoutRuleType.FIXED_AMOUNT,
                new BigDecimal("2000.0000"),
                null,
                null,
                "terminal promo payout",
                null));

    assertThat(result.created()).isTrue();
    assertThat(store.saved)
        .singleElement()
        .satisfies(
            override -> {
              assertThat(override.gameCode()).isEqualTo("HT_MARYAJ_GRATIS");
              assertThat(override.payoutRuleType()).isEqualTo(PayoutRuleType.FIXED_AMOUNT);
              assertThat(override.fixedAmount()).isEqualByComparingTo("2000.0000");
              assertThat(override.odds()).isEqualByComparingTo("1.0000");
            });
  }

  @Test
  void upsertRejectsPaidGameFixedAmountOverride() {
    tenantPricingStore.saved.add(
        tenantDefault(
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_STRAIGHT,
            PayoutRuleType.STAKE_MULTIPLIER,
            new BigDecimal("5000.0000"),
            null));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new UpsertSellerTerminalPricingRuleOverrideCommand(
                        TENANT_ID,
                        SELLER_TERMINAL_ID,
                        "HT_LOTO4",
                        PricingVariantCode.LOTTO4_STRAIGHT,
                        "LOTTO4_PATTERN",
                        (short) 1,
                        null,
                        PayoutRuleType.FIXED_AMOUNT,
                        new BigDecimal("5000.0000"),
                        null,
                        null,
                        "bad override",
                        null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires payoutRuleType STAKE_MULTIPLIER");
  }

  @Test
  void upsertRejectsMaryajGratisMultiplierOverride() {
    tenantPricingStore.saved.add(
        tenantDefault(
            "HT_MARYAJ_GRATIS",
            PricingVariantCode.MARRIAGE_EXACT_ORDER,
            PayoutRuleType.FIXED_AMOUNT,
            BigDecimal.ONE,
            new BigDecimal("1000.0000")));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new UpsertSellerTerminalPricingRuleOverrideCommand(
                        TENANT_ID,
                        SELLER_TERMINAL_ID,
                        "HT_MARYAJ_GRATIS",
                        PricingVariantCode.MARRIAGE_EXACT_ORDER,
                        "MARRIAGE_2D2D",
                        (short) 1,
                        new BigDecimal("50.0000"),
                        PayoutRuleType.STAKE_MULTIPLIER,
                        null,
                        null,
                        null,
                        "bad override",
                        null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires payoutRuleType FIXED_AMOUNT");
  }

  @Test
  void upsertRejectsMissingTenantDefaultRule() {
    assertThatThrownBy(
            () ->
                handler.handle(
                    new UpsertSellerTerminalPricingRuleOverrideCommand(
                        TENANT_ID,
                        SELLER_TERMINAL_ID,
                        "HT_MARYAJ_GRATIS",
                        PricingVariantCode.MARRIAGE_EXACT_ORDER,
                        "MARRIAGE_2D2D",
                        (short) 1,
                        null,
                        PayoutRuleType.FIXED_AMOUNT,
                        new BigDecimal("2000.0000"),
                        null,
                        null,
                        "orphan override",
                        null)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("tenant default pricing rule missing");
  }

  private static TenantPricingOdds tenantDefault(
      String gameCode,
      PricingVariantCode pricingVariantCode,
      PayoutRuleType payoutRuleType,
      BigDecimal odds,
      BigDecimal fixedAmount) {
    return new TenantPricingOdds(
        TENANT_ID,
        gameCode,
        pricingVariantCode,
        pricingVariantCode == PricingVariantCode.LOTTO4_STRAIGHT
            ? "LOTTO4_PATTERN"
            : "MARRIAGE_2D2D",
        pricingVariantCode == PricingVariantCode.LOTTO4_STRAIGHT ? (short) 1 : (short) 2,
        odds,
        payoutRuleType,
        fixedAmount,
        true,
        null);
  }

  private static final class InMemorySellerTerminalOverrideStore
      implements SellerTerminalOddsOverrideReaderPort, SellerTerminalOddsOverrideWriterPort {

    private final List<SellerTerminalOddsOverride> saved = new ArrayList<>();

    @Override
    public Optional<SellerTerminalOddsOverride> findById(SellerTerminalOddsOverrideId id) {
      return saved.stream().filter(override -> override.id().equals(id)).findFirst();
    }

    @Override
    public List<SellerTerminalOddsOverride> findActiveBySellerTerminal(
        TenantId tenantId, SellerTerminalId sellerTerminalId) {
      return saved.stream()
          .filter(SellerTerminalOddsOverride::active)
          .filter(override -> override.tenantId().equals(tenantId))
          .filter(override -> override.sellerTerminalId().equals(sellerTerminalId))
          .toList();
    }

    @Override
    public Optional<SellerTerminalOddsOverride> findActiveByNaturalKey(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        PricingVariantCode pricingVariantCode) {
      return saved.stream()
          .filter(SellerTerminalOddsOverride::active)
          .filter(override -> override.tenantId().equals(tenantId))
          .filter(override -> override.sellerTerminalId().equals(sellerTerminalId))
          .filter(override -> override.gameCode().equals(gameCode))
          .filter(override -> override.pricingVariantCode() == pricingVariantCode)
          .findFirst();
    }

    @Override
    public SellerTerminalOddsOverride save(SellerTerminalOddsOverride override) {
      findById(override.id()).ifPresent(saved::remove);
      saved.add(override);
      return override;
    }

    @Override
    public void delete(SellerTerminalOddsOverride override) {
      saved.remove(override);
    }
  }

  private static final class InMemoryTenantPricingStore implements TenantPricingOddsReaderPort {

    private final List<TenantPricingOdds> saved = new ArrayList<>();

    @Override
    public List<TenantPricingOdds> findActiveByTenant(TenantId tenantId, String gameCode) {
      return saved.stream()
          .filter(TenantPricingOdds::active)
          .filter(rule -> rule.tenantId().equals(tenantId))
          .filter(rule -> gameCode == null || rule.gameCode().equals(gameCode))
          .toList();
    }

    @Override
    public Optional<TenantPricingOdds> findByNaturalKey(
        TenantId tenantId, String gameCode, PricingVariantCode pricingVariantCode) {
      return saved.stream()
          .filter(TenantPricingOdds::active)
          .filter(rule -> rule.tenantId().equals(tenantId))
          .filter(rule -> rule.gameCode().equals(gameCode))
          .filter(rule -> rule.pricingVariantCode() == pricingVariantCode)
          .findFirst();
    }
  }
}
