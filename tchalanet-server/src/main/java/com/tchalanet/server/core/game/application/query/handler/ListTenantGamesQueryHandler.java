package com.tchalanet.server.core.game.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.game.application.port.out.ListTenantGamesPort;
import com.tchalanet.server.core.game.application.query.model.ListTenantGamesQuery;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListTenantGamesQueryHandler
    implements QueryHandler<
        ListTenantGamesQuery,
        java.util.List<com.tchalanet.server.core.game.domain.model.TenantGame>> {

  private final ListTenantGamesPort port;

  @Override
  public java.util.List<com.tchalanet.server.core.game.domain.model.TenantGame> handle(
      ListTenantGamesQuery query) {
    return port.listAll();
  }
}
