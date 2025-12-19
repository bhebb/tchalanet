package com.tchalanet.server.core.payout.port.out;

import com.tchalanet.server.core.payout.domain.model.Payout;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRepositoryPort {
  Payout save(Payout payout);

  Optional<Payout> findByTicketId(UUID ticketId);

  Optional<Payout> findById(UUID payoutId);
}

