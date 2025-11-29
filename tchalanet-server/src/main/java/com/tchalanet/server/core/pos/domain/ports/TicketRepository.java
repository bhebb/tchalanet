package com.tchalanet.server.core.pos.domain.ports;

import com.tchalanet.server.core.pos.domain.model.Ticket;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {
  Optional<Ticket> findById(UUID id);

  Ticket save(Ticket t);
}
