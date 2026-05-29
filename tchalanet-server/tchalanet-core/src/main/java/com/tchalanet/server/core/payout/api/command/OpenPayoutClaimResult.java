package com.tchalanet.server.core.payout.api.command;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;

public record OpenPayoutClaimResult(
    PayoutId payoutId,
    PayoutClaimStatus status,
    boolean alreadyExisted
) {}
