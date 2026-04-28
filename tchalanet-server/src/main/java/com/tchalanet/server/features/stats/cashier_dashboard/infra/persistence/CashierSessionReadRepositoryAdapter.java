package com.tchalanet.server.features.stats.cashier_dashboard.infra.persistence;

import com.tchalanet.server.core.session.infra.persistence.repository.SalesSessionJpaRepository;
import com.tchalanet.server.features.stats.cashier_dashboard.application.CashierSessionReadRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashierSessionReadRepositoryAdapter implements CashierSessionReadRepository {

  private final SalesSessionJpaRepository posSessionJpaRepository;

  @Override
  public boolean hasOpenSession(UUID tenantId, UUID cashierId) {
    var open = posSessionJpaRepository.findOpenByCashier(tenantId, cashierId);
    return open != null && !open.isEmpty();
  }
}
