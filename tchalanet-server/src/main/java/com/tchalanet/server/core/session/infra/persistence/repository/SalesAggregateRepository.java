package com.tchalanet.server.core.session.infra.persistence.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface SalesAggregateRepository {

  SalesAgg computeTicketAgg(UUID tenantId, UUID sessionId);

  record SalesAgg(long totalTickets, BigDecimal totalStake) {}
}
