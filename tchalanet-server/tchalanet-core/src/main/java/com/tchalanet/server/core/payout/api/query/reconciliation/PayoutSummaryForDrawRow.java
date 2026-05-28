package com.tchalanet.server.core.payout.api.query.reconciliation;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.Money;

public record PayoutSummaryForDrawRow(
    DrawId drawId,
    long claimCount,
    long approvedClaimCount,
    long rejectedClaimCount,
    long paidPaymentCount,
    Money claimedAmount,
    Money paidAmount
) {}
