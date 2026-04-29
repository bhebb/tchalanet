package com.tchalanet.server.features.stats.cashierdashboard.persistence;

import com.tchalanet.server.core.user.infra.persistence.JpaAppUserRepository;
import com.tchalanet.server.features.stats.cashierdashboard.model.CashierInfoProjection;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashierReaderDataSource implements CashierReader {

  private final JpaAppUserRepository jpaAppUserRepository;

  @Override
  public Optional<CashierInfoProjection> findInfoById(UUID tenantId, UUID cashierId) {
    return jpaAppUserRepository
        .findById(cashierId)
        .map(
            u ->
                new CashierInfoProjection(
                    u.getId(),
                    u.getDisplayName() != null ? u.getDisplayName() : u.getUsername(),
                    null,
                    null));
  }
}
