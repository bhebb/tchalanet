package com.tchalanet.server.core.session.infra.persistence.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface PayoutAggregateRepository {

  BigDecimal computePayoutAgg(UUID tenantId, UUID sessionId);
}
