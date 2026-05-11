package com.tchalanet.server.core.payout.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.PayoutId;
import jakarta.validation.constraints.NotNull;

public record GetPayoutDetailsQuery(
    @NotNull PayoutId payoutId
) implements Query<PayoutDetails> {}
