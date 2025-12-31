package com.tchalanet.server.core.game.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.game.application.port.out.ListTenantGamesPort;
import com.tchalanet.server.core.game.application.query.model.FindTenantGameByCodeQuery;
import com.tchalanet.server.core.game.domain.model.TenantGame;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class FindTenantGameByCodeQueryHandler
    implements QueryHandler<FindTenantGameByCodeQuery, Optional<TenantGame>> {

  private final ListTenantGamesPort port;

  @Override
  public Optional<TenantGame> handle(FindTenantGameByCodeQuery query) {
    return port.findByGameCode(query.code());
  }
}
