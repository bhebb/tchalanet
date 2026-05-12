package com.tchalanet.server.core.sales.internal.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketLineJpaRepository extends JpaRepository<TicketLineJpaEntity, UUID> {
  List<TicketLineJpaEntity> findByTicketIdOrderByLineNo(UUID ticketId);
}
