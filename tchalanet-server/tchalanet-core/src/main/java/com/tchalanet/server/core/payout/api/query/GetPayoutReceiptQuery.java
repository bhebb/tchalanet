package com.tchalanet.server.core.payout.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.PayoutId;
import jakarta.validation.constraints.NotNull;

public record GetPayoutReceiptQuery(
    @NotNull PayoutId payoutId
) implements Query<PayoutReceiptView> {}
