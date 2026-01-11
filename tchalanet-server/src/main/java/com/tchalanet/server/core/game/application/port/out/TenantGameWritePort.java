package com.tchalanet.server.core.game.application.port.out;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.core.game.application.command.model.UpdateTenantGameCommand;

public interface TenantGameWritePort {
  boolean updateByGameId(GameId gameId, UpdateTenantGameCommand cmd);
}
