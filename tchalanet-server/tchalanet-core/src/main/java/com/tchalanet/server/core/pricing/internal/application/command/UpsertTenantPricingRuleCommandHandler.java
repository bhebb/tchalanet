package com.tchalanet.server.core.pricing.internal.application.command;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pricing.api.command.UpsertTenantPricingRuleCommand;
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.TenantPricingRuleView;
import com.tchalanet.server.core.pricing.internal.application.mapper.TenantPricingOddsMapper;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.TenantPricingOddsWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.TenantPricingOdds;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@UseCase
@RequiredArgsConstructor
public class UpsertTenantPricingRuleCommandHandler implements CommandHandler<UpsertTenantPricingRuleCommand, TenantPricingRuleView> {

    private final TenantPricingOddsReaderPort reader;
    private final TenantPricingOddsWriterPort writer;
    private final TenantPricingOddsMapper mapper;

    @Override
    @TchTx
    public TenantPricingRuleView handle(UpsertTenantPricingRuleCommand c) {
        var gameCode = TenantPricingOdds.normalizeGameCode(c.gameCode());
        var betType = TenantPricingOdds.normalizeBetType(c.betType());

        var ruleType = c.payoutRuleType() == null ? PayoutRuleType.STAKE_MULTIPLIER : c.payoutRuleType();
        validate(gameCode, ruleType, c.odds(), c.fixedAmount());
        var odds = effectiveOdds(ruleType, c.odds());

        var pricing = reader.findByNaturalKey(c.tenantId(), gameCode, c.pricingVariantCode())
            .map(existing -> existing.update(odds, betType, c.betOption(), ruleType, c.fixedAmount()))
            .orElseGet(() -> new TenantPricingOdds(
                c.tenantId(),
                gameCode,
                c.pricingVariantCode(),
                betType,
                c.betOption(),
                odds,
                ruleType,
                c.fixedAmount(),
                true,
                null
            ));

        return mapper.toView(writer.save(pricing));
    }

    private void validate(String gameCode, PayoutRuleType ruleType, BigDecimal odds, BigDecimal fixedAmount) {
        PricingRuleV0Policy.validateGameRuleType(gameCode, ruleType);
        if (ruleType == PayoutRuleType.STAKE_MULTIPLIER && (odds == null || odds.signum() <= 0)) {
            throw new IllegalArgumentException("odds must be positive");
        }
        if (ruleType == PayoutRuleType.FIXED_AMOUNT && (fixedAmount == null || fixedAmount.signum() < 0)) {
            throw new IllegalArgumentException("fixedAmount must be non-negative");
        }
    }

    private BigDecimal effectiveOdds(PayoutRuleType ruleType, BigDecimal odds) {
        if (ruleType == PayoutRuleType.FIXED_AMOUNT && odds == null) {
            return BigDecimal.ONE;
        }
        return odds;
    }
}
