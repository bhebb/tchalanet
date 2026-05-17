package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.internal.infra.persistence.view.TicketPrintHeaderViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// JpaRepository is sufficient here because this is a read-only SQL view projection.
public interface TicketPrintHeaderViewRepository extends JpaRepository<TicketPrintHeaderViewEntity, UUID> {

    default TicketPrintHeaderViewEntity getRequired(UUID ticketId) {
        return findById(ticketId)
            .orElseThrow(() -> ProblemRest.notFound("ticket.print_view.not_found", ticketId));
    }
}



