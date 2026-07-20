package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketChargeJpaEntity;
import java.util.List;
import java.util.UUID;

public interface TicketChargeJpaRepository extends TchJpaRepository<TicketChargeJpaEntity, UUID> {

  List<TicketChargeJpaEntity> findByTicket_IdOrderByChargeType(UUID ticketId);

  List<TicketChargeJpaEntity> findByTicket_IdInOrderByTicket_IdAscChargeTypeAsc(
      List<UUID> ticketIds);
}
