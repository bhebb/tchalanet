package com.tchalanet.server.core.session.internal.domain.service;

import com.tchalanet.server.core.session.domain.model.SalesSessionCashSummary;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SessionCashCalculator {

    public static SalesSessionCashSummary calculate(
        Long openingFloatCents,
        Long salesCashInCents,
        Long payoutCashOutCents,
        Long declaredClosingAmountCents) {

        var expected =
            nz(openingFloatCents)
                + nz(salesCashInCents)
                - nz(payoutCashOutCents);

        var variance =
            declaredClosingAmountCents == null
                ? null
                : declaredClosingAmountCents - expected;

        return new SalesSessionCashSummary(
            openingFloatCents,
            salesCashInCents,
            payoutCashOutCents,
            expected,
            declaredClosingAmountCents,
            variance);
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }
}
