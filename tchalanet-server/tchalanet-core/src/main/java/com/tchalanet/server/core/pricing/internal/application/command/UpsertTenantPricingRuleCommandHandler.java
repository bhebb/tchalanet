package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.pricing.api.command.UpsertTenantPricingRuleCommand;
import com.tchalanet.server.core.pricing.api.error.PricingErrorCodes;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.TenantPricingRuleView;
import com.tchalanet.server.core.pricing.internal.application.mapper.TenantPricingOddsMapper;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpsertTenantPricingRuleCommandHandler
    implements CommandHandler<UpsertTenantPricingRuleCommand, TenantPricingRuleView> {

  private final TenantPricingOddsReaderPort reader;
  private final TenantPricingOddsWriterPort writer;
  private final TenantPricingOddsMapper mapper;

  @Override
  @TchTx
  public TenantPricingRuleView handle(UpsertTenantPricingRuleCommand c) {
    validateRequired(c.gameCode(), c.pricingVariantCode(), c.betType());
    var gameCode = TenantPricingOdds.normalizeGameCode(c.gameCode());
    var betType = TenantPricingOdds.normalizeBetType(c.betType());

    var ruleType =
        c.payoutRuleType() == null ? PayoutRuleType.STAKE_MULTIPLIER : c.payoutRuleType();
    validate(gameCode, ruleType, c.odds(), c.fixedAmount());
    var odds = effectiveOdds(ruleType, c.odds());

    var pricing =
        reader
            .findByNaturalKey(c.tenantId(), gameCode, c.pricingVariantCode())
            .map(
                existing ->
                    existing.update(odds, betType, c.betOption(), ruleType, c.fixedAmount()))
            .orElseGet(
                () ->
                    new TenantPricingOdds(
                        c.tenantId(),
                        gameCode,
                        c.pricingVariantCode(),
                        betType,
                        c.betOption(),
                        odds,
                        ruleType,
                        c.fixedAmount(),
                        true,
                        null));

    return mapper.toView(writer.save(pricing));
  }

  private void validate(
      String gameCode, PayoutRuleType ruleType, BigDecimal odds, BigDecimal fixedAmount) {
    if (ruleType != PricingRuleV0Policy.expectedRuleType(gameCode)) {
      throw ProblemRest.of(PricingErrorCodes.PAYOUT_RULE_TYPE_INVALID);
    }
    if (ruleType == PayoutRuleType.STAKE_MULTIPLIER && (odds == null || odds.signum() <= 0)) {
      throw ProblemRest.of(PricingErrorCodes.ODDS_INVALID);
    }
    if (ruleType == PayoutRuleType.FIXED_AMOUNT
        && (fixedAmount == null || fixedAmount.signum() < 0)) {
      throw ProblemRest.of(PricingErrorCodes.FIXED_AMOUNT_INVALID);
    }
  }

  private void validateRequired(
      String gameCode,
      com.tchalanet.server.core.pricing.api.model.PricingVariantCode pricingVariantCode,
      String betType) {
    if (gameCode == null || gameCode.isBlank()) {
      throw ProblemRest.of(PricingErrorCodes.GAME_CODE_REQUIRED);
    }
    if (pricingVariantCode == null) {
      throw ProblemRest.of(PricingErrorCodes.PRICING_VARIANT_REQUIRED);
    }
    if (betType == null || betType.isBlank()) {
      throw ProblemRest.of(PricingErrorCodes.BET_TYPE_REQUIRED);
    }
  }

  private BigDecimal effectiveOdds(PayoutRuleType ruleType, BigDecimal odds) {
    if (ruleType == PayoutRuleType.FIXED_AMOUNT && odds == null) {
      return BigDecimal.ONE;
    }
    return odds;
  }
}
