package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.platform.tenantgame.api.model.ResolveTenantGamesQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ResolveTenantGamesQueryHandler {

  private final TenantGamePersistencePort persistencePort;

  @Transactional(readOnly = true)
  public List<TenantGame> handle(ResolveTenantGamesQuery query) {
    return persistencePort.findAllByTenantId(query.getTenantId());
  }
}
