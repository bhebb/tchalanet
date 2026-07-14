package com.tchalanet.server.core.pricing.internal.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pricing.api.model.OddsSource;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalPayoutRuleQuery;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.domain.SellerTerminalOddsOverride;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolveSellerTerminalPayoutRuleQueryHandlerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final SellerTerminalId SELLER_TERMINAL_ID =
      SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));

  @Test
  void resolvesTenantFixedAmountRule() {
    var handler =
        new ResolveSellerTerminalPayoutRuleQueryHandler(
            new NoOverrideReader(), new FixedTenantPricingReader(new BigDecimal("500.0000")));

    var result = handler.handle(query());

    assertThat(result.effectiveRuleType()).isEqualTo(PayoutRuleType.FIXED_AMOUNT);
    assertThat(result.effectiveFixedAmount()).isEqualByComparingTo("500.0000");
    assertThat(result.effectiveMultiplier()).isEqualByComparingTo("1.0000");
    assertThat(result.source()).isEqualTo(OddsSource.TENANT_DEFAULT);
  }

  @Test
  void rejectsSellerTerminalOverrideThatChangesRuleType() {
    var handler =
        new ResolveSellerTerminalPayoutRuleQueryHandler(
            new MultiplierOverrideReader(),
            new FixedTenantPricingReader(new BigDecimal("500.0000")));

    assertThatThrownBy(() -> handler.handle(query()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("cannot change payout rule type");
  }

  @Test
  void fallsBackToTenantDefaultWhenOnlySiblingVariantIsOverridden() {
    var tenantReader =
        new VariantTenantPricingReader(
            Map.of(
                PricingVariantCode.MARRIAGE_EXACT_ORDER, new BigDecimal("1000.0000"),
                PricingVariantCode.MARRIAGE_REVERSE_ALLOWED, new BigDecimal("1500.0000")));
    var overrideReader =
        new SingleFixedOverrideReader(
            PricingVariantCode.MARRIAGE_EXACT_ORDER, new BigDecimal("2000.0000"));
    var handler = new ResolveSellerTerminalPayoutRuleQueryHandler(overrideReader, tenantReader);

    var reverse =
        handler.handle(
            new ResolveSellerTerminalPayoutRuleQuery(
                TENANT_ID,
                SELLER_TERMINAL_ID,
                "HT_MARYAJ_GRATIS",
                PricingVariantCode.MARRIAGE_REVERSE_ALLOWED,
                "MARRIAGE_2D2D",
                (short) 2));
    var exact =
        handler.handle(
            new ResolveSellerTerminalPayoutRuleQuery(
                TENANT_ID,
                SELLER_TERMINAL_ID,
                "HT_MARYAJ_GRATIS",
                PricingVariantCode.MARRIAGE_EXACT_ORDER,
                "MARRIAGE_2D2D",
                (short) 1));

    assertThat(reverse.source()).isEqualTo(OddsSource.TENANT_DEFAULT);
    assertThat(reverse.effectiveFixedAmount()).isEqualByComparingTo("1500.0000");
    assertThat(exact.source()).isEqualTo(OddsSource.SELLER_TERMINAL_OVERRIDE);
    assertThat(exact.effectiveFixedAmount()).isEqualByComparingTo("2000.0000");
  }

  private static ResolveSellerTerminalPayoutRuleQuery query() {
    return new ResolveSellerTerminalPayoutRuleQuery(
        TENANT_ID,
        SELLER_TERMINAL_ID,
        "HT_MARYAJ_GRATIS",
        PricingVariantCode.MARRIAGE_REVERSE_ALLOWED,
        "MARRIAGE_2D2D",
        (short) 2);
  }

  private static final class FixedTenantPricingReader implements TenantPricingOddsReaderPort {
    private final BigDecimal fixedAmount;

    private FixedTenantPricingReader(BigDecimal fixedAmount) {
      this.fixedAmount = fixedAmount;
    }

    @Override
    public List<TenantPricingOdds> findActiveByTenant(TenantId tenantId, String gameCode) {
      return List.of();
    }

    @Override
    public Optional<TenantPricingOdds> findByNaturalKey(
        TenantId tenantId, String gameCode, PricingVariantCode pricingVariantCode) {
      return Optional.of(
          new TenantPricingOdds(
              tenantId,
              gameCode,
              pricingVariantCode,
              "MARRIAGE_2D2D",
              (short) 2,
              BigDecimal.ONE,
              PayoutRuleType.FIXED_AMOUNT,
              fixedAmount,
              true,
              null));
    }
  }

  private static class NoOverrideReader implements SellerTerminalOddsOverrideReaderPort {
    @Override
    public Optional<SellerTerminalOddsOverride> findById(SellerTerminalOddsOverrideId id) {
      return Optional.empty();
    }

    @Override
    public List<SellerTerminalOddsOverride> findActiveBySellerTerminal(
        TenantId tenantId, SellerTerminalId sellerTerminalId) {
      return List.of();
    }

    @Override
    public Optional<SellerTerminalOddsOverride> findActiveByNaturalKey(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        PricingVariantCode pricingVariantCode) {
      return Optional.empty();
    }
  }

  private static final class MultiplierOverrideReader extends NoOverrideReader {
    @Override
    public Optional<SellerTerminalOddsOverride> findActiveByNaturalKey(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        PricingVariantCode pricingVariantCode) {
      return Optional.of(
          SellerTerminalOddsOverride.createNew(
              SellerTerminalOddsOverrideId.of(
                  UUID.fromString("30000000-0000-0000-0000-000000000001")),
              tenantId,
              sellerTerminalId,
              gameCode,
              pricingVariantCode,
              "MARRIAGE_2D2D",
              (short) 2,
              new BigDecimal("2.0000"),
              null,
              null,
              null,
              null));
    }
  }

  private static final class VariantTenantPricingReader implements TenantPricingOddsReaderPort {
    private final Map<PricingVariantCode, BigDecimal> fixedAmounts;

    private VariantTenantPricingReader(Map<PricingVariantCode, BigDecimal> fixedAmounts) {
      this.fixedAmounts = new EnumMap<>(fixedAmounts);
    }

    @Override
    public List<TenantPricingOdds> findActiveByTenant(TenantId tenantId, String gameCode) {
      return List.of();
    }

    @Override
    public Optional<TenantPricingOdds> findByNaturalKey(
        TenantId tenantId, String gameCode, PricingVariantCode pricingVariantCode) {
      var fixedAmount = fixedAmounts.get(pricingVariantCode);
      if (fixedAmount == null) {
        return Optional.empty();
      }
      return Optional.of(
          new TenantPricingOdds(
              tenantId,
              gameCode,
              pricingVariantCode,
              "MARRIAGE_2D2D",
              pricingVariantCode == PricingVariantCode.MARRIAGE_EXACT_ORDER ? (short) 1 : (short) 2,
              BigDecimal.ONE,
              PayoutRuleType.FIXED_AMOUNT,
              fixedAmount,
              true,
              null));
    }
  }

  private static final class SingleFixedOverrideReader extends NoOverrideReader {
    private final PricingVariantCode overriddenVariant;
    private final BigDecimal fixedAmount;

    private SingleFixedOverrideReader(
        PricingVariantCode overriddenVariant, BigDecimal fixedAmount) {
      this.overriddenVariant = overriddenVariant;
      this.fixedAmount = fixedAmount;
    }

    @Override
    public Optional<SellerTerminalOddsOverride> findActiveByNaturalKey(
        TenantId tenantId,
        SellerTerminalId sellerTerminalId,
        String gameCode,
        PricingVariantCode pricingVariantCode) {
      if (pricingVariantCode != overriddenVariant) {
        return Optional.empty();
      }
      return Optional.of(
          SellerTerminalOddsOverride.createNew(
              SellerTerminalOddsOverrideId.of(
                  UUID.fromString("30000000-0000-0000-0000-000000000001")),
              tenantId,
              sellerTerminalId,
              gameCode,
              pricingVariantCode,
              "MARRIAGE_2D2D",
              pricingVariantCode == PricingVariantCode.MARRIAGE_EXACT_ORDER ? (short) 1 : (short) 2,
              BigDecimal.ONE,
              PayoutRuleType.FIXED_AMOUNT,
              fixedAmount,
              null,
              null,
              "partial override",
              null));
    }
  }
}
