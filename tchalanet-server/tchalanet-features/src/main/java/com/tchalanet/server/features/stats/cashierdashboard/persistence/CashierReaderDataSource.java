package com.tchalanet.server.features.stats.cashierdashboard.persistence;

import com.tchalanet.server.features.stats.cashierdashboard.model.CashierInfoProjection;
import com.tchalanet.server.platform.identity.api.IdentityApi;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashierReaderDataSource implements CashierReader {

  private final IdentityApi identityApi;

  @Override
  public Optional<CashierInfoProjection> findInfoById(UUID tenantId, UUID cashierId) {
    return identityApi
        .findAppUser(cashierId)
        .map(
            u ->
                new CashierInfoProjection(
                    u.id().value(),
                    u.displayName() != null ? u.displayName() : u.username(),
                    null,
                    null));
  }
}
