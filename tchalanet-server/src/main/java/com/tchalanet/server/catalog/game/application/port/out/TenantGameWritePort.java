package com.tchalanet.server.catalog.game.application.port.out;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.catalog.game.application.command.model.UpdateTenantGameCommand;

public interface TenantGameWritePort {
  boolean updateByGameId(GameId gameId, UpdateTenantGameCommand cmd);
}
