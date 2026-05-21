package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketChargeJpaEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketChargeJpaRepository extends TchJpaRepository<TicketChargeJpaEntity, UUID> {

    List<TicketChargeJpaEntity> findByTicket_IdOrderByChargeType(UUID ticketId);

    /** See {@link TicketLineJpaRepository#findVersionsByTicketId}. */
    @Query("select c.id as id, c.version as version from TicketChargeJpaEntity c where c.ticket.id = :ticketId")
    List<TicketChargeVersionView> findVersionsByTicketId(@Param("ticketId") UUID ticketId);

    interface TicketChargeVersionView {
        UUID getId();
        long getVersion();
    }
}
