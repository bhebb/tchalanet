package com.tchalanet.server.core.limitpolicy.application.query.model.exposure;

import com.tchalanet.server.common.types.enums.BetType;

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
