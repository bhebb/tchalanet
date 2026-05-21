package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketLineJpaEntity;

import java.util.List;
import java.util.UUID;

public interface TicketLineJpaRepository extends TchJpaRepository<TicketLineJpaEntity, UUID> {

    List<TicketLineJpaEntity> findByTicketIdOrderByLineNumber(UUID ticketId);
}
