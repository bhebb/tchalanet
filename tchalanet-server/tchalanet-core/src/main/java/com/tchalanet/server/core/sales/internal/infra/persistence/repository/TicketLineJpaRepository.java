package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketLineJpaEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketLineJpaRepository extends TchJpaRepository<TicketLineJpaEntity, UUID> {

    List<TicketLineJpaEntity> findByTicketIdOrderByLineNumber(UUID ticketId);

    /**
     * Returns (id, version) tuples for all lines of a ticket. Used by TicketJpaAdapter#save to
     * transplant @Version values onto fresh entities rebuilt from the domain aggregate, so
     * Hibernate's optimistic-locking merge accepts the update.
     */
    @Query("select l.id as id, l.version as version from TicketLineJpaEntity l where l.ticket.id = :ticketId")
    List<TicketLineVersionView> findVersionsByTicketId(@Param("ticketId") UUID ticketId);

    interface TicketLineVersionView {
        UUID getId();
        long getVersion();
    }
}
