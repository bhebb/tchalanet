package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.pricing.api.command.EnsureDefaultHaitiLotteryPricingRulesCommand;
import com.tchalanet.server.core.pricing.api.error.PricingErrorCodes;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EnsureDefaultHaitiLotteryPricingRulesCommandHandler
    implements CommandHandler<EnsureDefaultHaitiLotteryPricingRulesCommand, Void> {

  private final TenantPricingOddsReaderPort reader;
  private final TenantPricingOddsWriterPort writer;

  @Override
  @TchTx
  public Void handle(EnsureDefaultHaitiLotteryPricingRulesCommand c) {
    if (c.tenantId() == null) {
      throw ProblemRest.of(PricingErrorCodes.TENANT_REQUIRED);
    }
    for (DefaultPricingRule row : defaultHaitiLotteryPricingRules()) {
      ensureRule(c.tenantId(), row);
    }
    return null;
  }

  private void ensureRule(TenantId tenantId, DefaultPricingRule row) {
    var rule =
        reader
            .findByNaturalKey(tenantId, row.gameCode(), row.pricingVariantCode())
            .map(
                existing ->
                    existing.update(
                        row.odds(),
                        row.betType(),
                        row.betOption(),
                        row.payoutRuleType(),
                        row.fixedAmount()))
            .orElseGet(
                () ->
                    new TenantPricingOdds(
                        tenantId,
                        row.gameCode(),
                        row.pricingVariantCode(),
                        row.betType(),
                        row.betOption(),
                        row.odds(),
                        row.payoutRuleType(),
                        row.fixedAmount(),
                        true,
                        null));
    writer.save(rule);
  }

  private static List<DefaultPricingRule> defaultHaitiLotteryPricingRules() {
    return List.of(
        multiplier("HT_BOLET", PricingVariantCode.MATCH_1_2D, "MATCH_1_2D", null, "50.0000"),
        multiplier("HT_BOLET", PricingVariantCode.MATCH_2_2D, "MATCH_2_2D", null, "20.0000"),
        multiplier("HT_BOLET", PricingVariantCode.MATCH_3_2D, "MATCH_3_2D", null, "10.0000"),
        multiplier(
            "HT_MARYAJ",
            PricingVariantCode.MARRIAGE_EXACT_ORDER,
            "MARRIAGE_2D2D",
            (short) 1,
            "1000.0000"),
        multiplier(
            "HT_MARYAJ",
            PricingVariantCode.MARRIAGE_REVERSE_ALLOWED,
            "MARRIAGE_2D2D",
            (short) 2,
            "1000.0000"),
        fixed(
            "HT_MARYAJ_GRATIS",
            PricingVariantCode.MARRIAGE_EXACT_ORDER,
            "MARRIAGE_2D2D",
            (short) 1,
            "500.0000"),
        fixed(
            "HT_MARYAJ_GRATIS",
            PricingVariantCode.MARRIAGE_REVERSE_ALLOWED,
            "MARRIAGE_2D2D",
            (short) 2,
            "500.0000"),
        multiplier(
            "HT_LOTO3", PricingVariantCode.LOTTO3_STRAIGHT, "LOTTO3_3D", (short) 1, "500.0000"),
        multiplier(
            "HT_LOTO3", PricingVariantCode.LOTTO3_BOX_3_WAY, "LOTTO3_3D", (short) 2, "500.0000"),
        multiplier(
            "HT_LOTO3", PricingVariantCode.LOTTO3_BOX_6_WAY, "LOTTO3_3D", (short) 2, "500.0000"),
        multiplier(
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_STRAIGHT,
            "LOTTO4_PATTERN",
            (short) 1,
            "5000.0000"),
        multiplier(
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BOX_4_WAY,
            "LOTTO4_PATTERN",
            (short) 2,
            "1200.0000"),
        multiplier(
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BOX_6_WAY,
            "LOTTO4_PATTERN",
            (short) 2,
            "800.0000"),
        multiplier(
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BOX_12_WAY,
            "LOTTO4_PATTERN",
            (short) 2,
            "400.0000"),
        multiplier(
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BOX_24_WAY,
            "LOTTO4_PATTERN",
            (short) 2,
            "200.0000"),
        multiplier(
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_FRONT_PAIR,
            "LOTTO4_PATTERN",
            (short) 3,
            "5000.0000"),
        multiplier(
            "HT_LOTO4",
            PricingVariantCode.LOTTO4_BACK_PAIR,
            "LOTTO4_PATTERN",
            (short) 4,
            "5000.0000"),
        multiplier(
            "HT_LOTO5",
            PricingVariantCode.LOTTO5_LOT1_LOT2,
            "LOTTO5_PATTERN",
            (short) 1,
            "25000.0000"),
        multiplier(
            "HT_LOTO5",
            PricingVariantCode.LOTTO5_LOT1_LOT3,
            "LOTTO5_PATTERN",
            (short) 2,
            "25000.0000"),
        multiplier(
            "HT_LOTO5",
            PricingVariantCode.LOTTO5_MIXED_1_2_3,
            "LOTTO5_PATTERN",
            (short) 3,
            "25000.0000"));
  }

  private static DefaultPricingRule multiplier(
      String gameCode,
      PricingVariantCode pricingVariantCode,
      String betType,
      Short betOption,
      String odds) {
    return new DefaultPricingRule(
        gameCode,
        pricingVariantCode,
        betType,
        betOption,
        new BigDecimal(odds),
        PayoutRuleType.STAKE_MULTIPLIER,
        null);
  }

  private static DefaultPricingRule fixed(
      String gameCode,
      PricingVariantCode pricingVariantCode,
      String betType,
      Short betOption,
      String fixedAmount) {
    return new DefaultPricingRule(
        gameCode,
        pricingVariantCode,
        betType,
        betOption,
        BigDecimal.ONE,
        PayoutRuleType.FIXED_AMOUNT,
        new BigDecimal(fixedAmount));
  }

  private record DefaultPricingRule(
      String gameCode,
      PricingVariantCode pricingVariantCode,
      String betType,
      Short betOption,
      BigDecimal odds,
      PayoutRuleType payoutRuleType,
      BigDecimal fixedAmount) {}
}
