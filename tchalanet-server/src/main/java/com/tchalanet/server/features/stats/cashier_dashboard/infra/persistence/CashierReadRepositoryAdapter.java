package com.tchalanet.server.features.stats.cashier_dashboard.infra.persistence;

import com.tchalanet.server.core.user.infra.persistence.JpaAppUserRepository;
import com.tchalanet.server.features.stats.cashier_dashboard.application.CashierInfoProjection;
import com.tchalanet.server.features.stats.cashier_dashboard.application.CashierReadRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashierReadRepositoryAdapter implements CashierReadRepository {

  private final JpaAppUserRepository jpaAppUserRepository;

  @Override
  public Optional<CashierInfoProjection> findInfoById(UUID tenantId, UUID cashierId) {
    return jpaAppUserRepository
        .findById(cashierId)
        .filter(u -> u.getTenantId() != null && u.getTenantId().equals(tenantId))
        .map(
            u ->
                new CashierInfoProjection(
                    u.getId(),
                    u.getDisplayName() != null ? u.getDisplayName() : u.getUsername(),
                    null,
                    null));
  }
}
