package com.tchalanet.server.core.game.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.game.application.port.out.ListTenantGamesPort;
import com.tchalanet.server.core.game.application.query.model.ListEnabledTenantGamesQuery;
import com.tchalanet.server.core.game.domain.model.TenantGame;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListEnabledTenantGamesQueryHandler
    implements QueryHandler<ListEnabledTenantGamesQuery, List<TenantGame>> {

  private final ListTenantGamesPort port;

  @Override
  public List<TenantGame> handle(ListEnabledTenantGamesQuery query) {
    return port.listEnabled();
  }
}
