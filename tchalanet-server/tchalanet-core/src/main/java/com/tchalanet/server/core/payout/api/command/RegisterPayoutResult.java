package com.tchalanet.server.core.payout.api.command;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutStatus;
import java.math.BigDecimal;
import java.util.List;

    public record RegisterPayoutResult(
        PayoutId payoutId,
        RegisterPayoutStatus status,
        PayoutStatus payoutStatus,
        BigDecimal amount,
        String currency,
        List<String> warnings) {

        public static RegisterPayoutResult blocked(
            BigDecimal amount,
            String currency,
            List<String> warnings) {

            return new RegisterPayoutResult(
                null,
                RegisterPayoutStatus.BLOCKED,
                null,
                amount,
                currency,
                warnings);
        }

        public static RegisterPayoutResult requested(
            PayoutId payoutId,
            PayoutStatus payoutStatus,
            BigDecimal amount,
            String currency) {

            return new RegisterPayoutResult(
                payoutId,
                RegisterPayoutStatus.REQUESTED,
                payoutStatus,
                amount,
                currency,
                List.of());
        }

        public static RegisterPayoutResult paidNow(
            PayoutId payoutId,
            PayoutStatus payoutStatus,
            BigDecimal amount,
            String currency) {

            return new RegisterPayoutResult(
                payoutId,
                RegisterPayoutStatus.PAID,
                payoutStatus,
                amount,
                currency,
                List.of());
        }
    }
