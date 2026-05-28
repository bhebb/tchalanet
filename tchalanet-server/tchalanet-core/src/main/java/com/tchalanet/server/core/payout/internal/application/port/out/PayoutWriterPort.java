package com.tchalanet.server.core.payout.internal.application.port.out;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaim;

public interface PayoutWriterPort {
    PayoutClaim save(PayoutClaim claim);

    PayoutClaim lockByIdForPayment(PayoutId payoutId);
}
