package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.catalog.game.api.model.BetType;

import java.math.BigDecimal;

public record ExposureAlertItemView(
    BetType betType,
    String selectionKey,
    BigDecimal stakeTotal,
    BigDecimal potentialPayoutTotal,
    long salesCount,
    BigDecimal maxStakeExposureLimit,
    BigDecimal maxPayoutExposureLimit,
    BigDecimal stakeRatio,
    BigDecimal payoutRatio
) {}
