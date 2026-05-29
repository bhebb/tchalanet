package com.tchalanet.server.core.payout.internal.application.port.out;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaim;

import java.util.Optional;

public interface PayoutReaderPort {
    Optional<PayoutClaim> findById(PayoutId payoutId);

    PayoutClaim getById(PayoutId payoutId);

    Optional<PayoutClaim> findByTicketId(TicketId ticketId);
}
