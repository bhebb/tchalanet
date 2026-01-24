package com.tchalanet.server.core.tenantgame.application.query.handler;

import com.tchalanet.server.core.tenantgame.application.port.TenantGamePersistencePort;
import com.tchalanet.server.core.tenantgame.application.query.model.ResolveTenantGamesQuery;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
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
