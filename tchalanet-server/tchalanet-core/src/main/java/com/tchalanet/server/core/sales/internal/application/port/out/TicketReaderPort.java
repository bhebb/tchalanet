package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sales.api.query.AgentDailySalesDto;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketReaderPort {
    Optional<Ticket> findById(TicketId ticketId);

    Ticket getById(TicketId ticketId);

    boolean existsAcceptedOfflineCode(String offlineCode);

    boolean existsAcceptedLocalSequence(String terminalKey, long localSequence);

    Optional<Ticket> findByPublicCode(String publicCode);

    Optional<Ticket> findWithLinesById(TicketId ticketId);

    // search tenant-scoped: tenantId supprimé du filter
    TchPage<Ticket> search(ListTicketsQuery.TicketFilter filter, Pageable pageRequest);

    // CSV tenant-scoped: pas de tenantId
    byte[] exportDailySalesCsv(Instant dayStart, Instant dayEnd);

    List<Ticket> listRecentForCashier(UserId cashierId, int limit);

    // idem
    List<AgentDailySalesDto> getAgentDailySales(Instant from, Instant to);

}
