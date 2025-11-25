package com.tchalanet.server.pos.domain.ports;

import com.tchalanet.server.pos.domain.model.TicketLine;
import com.tchalanet.server.pos.domain.model.TicketLineId;
import java.util.Optional;

public interface TicketLineRepository {
  Optional<TicketLine> findById(TicketLineId id);

  TicketLine save(TicketLine l);
}
