package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.application.query.model.AgentDailySalesDto;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PageRequest;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PagedResult;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketFilter;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Outbound Port for ticket persistence. */
public interface TicketReaderPort {

  Optional<Ticket> findById(TicketId ticketId);

  Optional<Ticket> findByPublicCode(String publicCode);

  PagedResult<Ticket> search(TicketFilter filter, PageRequest pageRequest);

  Optional<Ticket> findWithLinesById(TicketId ticketId);

  List<Ticket> listRecentForCashier(UserId cashierId, int limit);

  List<AgentDailySalesDto> getAgentDailySales(TenantId tenantId, Instant from, Instant to);

  byte[] exportDailySalesCsv(TenantId tenantId, Instant dayStart, Instant dayEnd);
}
