package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PageRequest;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PagedResult;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketFilter;
import com.tchalanet.server.core.sales.domain.model.Ticket;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Outbound Port for ticket persistence. */
public interface TicketReaderPort {

  Optional<Ticket> findById(UUID ticketId);

  Optional<Ticket> findByPublicCode(String publicCode);

  PagedResult<Ticket> search(TicketFilter filter, PageRequest pageRequest);

}
