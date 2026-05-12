package com.tchalanet.server.core.payout.internal.application.port.out;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.payout.internal.domain.model.Payout;

import java.util.Optional;

public interface PayoutReaderPort {
    Optional<Payout> findById(PayoutId payoutId);

    Payout getById(PayoutId payoutId);

    Optional<Payout> findByTicketId(TicketId ticketId);
}
