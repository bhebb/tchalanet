package com.tchalanet.server.core.pos.domain.ports;

import com.tchalanet.server.core.pos.domain.model.TicketLine;
import com.tchalanet.server.core.pos.domain.model.TicketLineId;
import java.util.Optional;

public interface TicketLineRepository {
  Optional<TicketLine> findById(TicketLineId id);

  TicketLine save(TicketLine l);
}
