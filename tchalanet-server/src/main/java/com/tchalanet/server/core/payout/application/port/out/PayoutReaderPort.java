package com.tchalanet.server.core.payout.application.port.out;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.payout.domain.model.Payout;
import java.util.Optional;

public interface PayoutReaderPort {

  Optional<Payout> findByTicketId(TicketId ticketId);

  Optional<Payout> findById(PayoutId payoutId);
}
