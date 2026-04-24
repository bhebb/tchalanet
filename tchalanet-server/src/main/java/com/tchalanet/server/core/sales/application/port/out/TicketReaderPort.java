package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sales.application.query.model.AgentDailySalesDto;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketFilter;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Outbound Port for ticket persistence. */
public interface TicketReaderPort {
    Optional<Ticket> findById(TicketId ticketId);
    Optional<Ticket> findByPublicCode(String publicCode);

    Optional<Ticket> findWithLinesById(TicketId ticketId);

    TicketPrintView getTicketPrintView(TicketId ticketId);

    // search tenant-scoped: tenantId supprimé du filter
    TchPage<Ticket> search(TicketFilter filter, Pageable pageRequest);

    // CSV tenant-scoped: pas de tenantId
    byte[] exportDailySalesCsv(Instant dayStart, Instant dayEnd);

    List<Ticket> listRecentForCashier(UserId cashierId, int limit);

    // idem
    List<AgentDailySalesDto> getAgentDailySales(Instant from, Instant to);
}
