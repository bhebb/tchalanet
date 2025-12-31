package com.tchalanet.server.core.game.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.game.application.port.out.ListTenantGamesPort;
import com.tchalanet.server.core.game.application.query.model.FindTenantGameByIdQuery;
import com.tchalanet.server.core.game.domain.model.TenantGame;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class FindTenantGameByIdQueryHandler
    implements QueryHandler<FindTenantGameByIdQuery, Optional<TenantGame>> {

  private final ListTenantGamesPort port;

  @Override
  public Optional<TenantGame> handle(FindTenantGameByIdQuery query) {
    return port.findByGameId(query.gameId());
  }
}
