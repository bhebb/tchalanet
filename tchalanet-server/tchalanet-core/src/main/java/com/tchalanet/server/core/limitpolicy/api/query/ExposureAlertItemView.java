package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.catalog.game.api.model.BetType;

import java.math.BigDecimal;

public record ExposureAlertItemView(
    BetType betType,
    String selectionKey,
    BigDecimal stakeTotal,
    BigDecimal settlementPayoutExposureTotal,
    long salesCount,
    BigDecimal maxStakeExposureLimit,
    BigDecimal maxSettlementPayoutExposureLimit,
    BigDecimal stakeRatio,
    BigDecimal settlementPayoutRatio
) {}
