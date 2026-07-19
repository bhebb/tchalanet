package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.error.SalesErrorCodes;
import com.tchalanet.server.core.sales.internal.infra.persistence.view.TicketPrintHeaderViewEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository is sufficient here because this is a read-only SQL view projection.
public interface TicketPrintHeaderViewRepository
    extends JpaRepository<TicketPrintHeaderViewEntity, UUID> {

  default TicketPrintHeaderViewEntity getRequired(UUID ticketId) {
    return findById(ticketId)
        .orElseThrow(() -> ProblemRest.of(SalesErrorCodes.TICKET_PRINT_VIEW_NOT_FOUND));
  }
}
