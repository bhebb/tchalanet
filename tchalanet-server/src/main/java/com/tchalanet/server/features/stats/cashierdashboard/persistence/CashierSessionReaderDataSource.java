package com.tchalanet.server.features.stats.cashierdashboard.persistence;

import com.tchalanet.server.core.session.infra.persistence.SalesSessionJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashierSessionReaderDataSource implements CashierSessionReader {

  private final SalesSessionJpaRepository posSessionJpaRepository;

  @Override
  public boolean hasOpenSession(UUID tenantId, UUID cashierId) {
    var open = posSessionJpaRepository.findCurrentOpenByUser(tenantId, cashierId);
    return open != null && !open.isEmpty();
  }
}
