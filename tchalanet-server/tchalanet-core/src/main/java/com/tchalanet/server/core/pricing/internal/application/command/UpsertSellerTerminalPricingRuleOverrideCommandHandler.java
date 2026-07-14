package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.core.pricing.api.command.UpsertSellerTerminalPricingRuleOverrideCommand;
import com.tchalanet.server.core.pricing.api.command.UpsertSellerTerminalPricingRuleOverrideResult;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideWriterPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.domain.SellerTerminalOddsOverride;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpsertSellerTerminalPricingRuleOverrideCommandHandler
    implements CommandHandler<
        UpsertSellerTerminalPricingRuleOverrideCommand,
        UpsertSellerTerminalPricingRuleOverrideResult> {

  private final SellerTerminalOddsOverrideReaderPort reader;
  private final SellerTerminalOddsOverrideWriterPort writer;
  private final TenantPricingOddsReaderPort tenantPricingReader;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public UpsertSellerTerminalPricingRuleOverrideResult handle(
      UpsertSellerTerminalPricingRuleOverrideCommand c) {
    validate(c);
    var gameCode = TenantPricingOdds.normalizeGameCode(c.gameCode());
    var betType = TenantPricingOdds.normalizeBetType(c.betType());
    var tenantDefault =
        tenantPricingReader
            .findByNaturalKey(c.tenantId(), gameCode, c.pricingVariantCode())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "tenant default pricing rule missing for tenant=%s game=%s variant=%s"
                            .formatted(c.tenantId(), gameCode, c.pricingVariantCode())));
    if (tenantDefault.payoutRuleType() != ruleType(c)) {
      throw new IllegalArgumentException(
          "seller terminal pricing override cannot change payout rule type for game=%s variant=%s"
              .formatted(gameCode, c.pricingVariantCode()));
    }

    var existing =
        reader.findActiveByNaturalKey(
            c.tenantId(), c.sellerTerminalId(), gameCode, c.pricingVariantCode());

    if (existing.isPresent()) {
      var updated =
          existing
              .get()
              .update(
                  effectiveOdds(c),
                  ruleType(c),
                  c.fixedAmount(),
                  c.effectiveFrom(),
                  c.effectiveTo(),
                  c.reason(),
                  c.actorId());
      var saved = writer.save(updated);
      return new UpsertSellerTerminalPricingRuleOverrideResult(saved.id(), false);
    }

    var created =
        SellerTerminalOddsOverride.createNew(
            SellerTerminalOddsOverrideId.of(idGenerator.newUuid()),
            c.tenantId(),
            c.sellerTerminalId(),
            gameCode,
            c.pricingVariantCode(),
            betType,
            c.betOption(),
            effectiveOdds(c),
            ruleType(c),
            c.fixedAmount(),
            c.effectiveFrom(),
            c.effectiveTo(),
            c.reason(),
            c.actorId());
    var saved = writer.save(created);
    return new UpsertSellerTerminalPricingRuleOverrideResult(saved.id(), true);
  }

  private void validate(UpsertSellerTerminalPricingRuleOverrideCommand c) {
    if (c.gameCode() == null || c.gameCode().isBlank()) {
      throw new IllegalArgumentException("gameCode is required");
    }
    if (c.pricingVariantCode() == null) {
      throw new IllegalArgumentException("pricingVariantCode is required");
    }
    if (c.betType() == null || c.betType().isBlank()) {
      throw new IllegalArgumentException("betType is required");
    }
    var gameCode = TenantPricingOdds.normalizeGameCode(c.gameCode());
    PricingRuleV0Policy.validateGameRuleType(gameCode, ruleType(c));
    if (ruleType(c) == PayoutRuleType.STAKE_MULTIPLIER
        && (c.odds() == null || c.odds().compareTo(BigDecimal.ZERO) <= 0)) {
      throw new IllegalArgumentException("odds must be positive");
    }
    if (ruleType(c) == PayoutRuleType.FIXED_AMOUNT
        && (c.fixedAmount() == null || c.fixedAmount().signum() < 0)) {
      throw new IllegalArgumentException("fixedAmount must be non-negative");
    }
  }

  private PayoutRuleType ruleType(UpsertSellerTerminalPricingRuleOverrideCommand c) {
    return c.payoutRuleType() == null ? PayoutRuleType.STAKE_MULTIPLIER : c.payoutRuleType();
  }

  private BigDecimal effectiveOdds(UpsertSellerTerminalPricingRuleOverrideCommand c) {
    if (ruleType(c) == PayoutRuleType.FIXED_AMOUNT && c.odds() == null) {
      return BigDecimal.ONE;
    }
    return c.odds();
  }
}
