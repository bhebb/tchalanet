package com.tchalanet.server.core.game.application.port.out;

import com.tchalanet.server.core.game.application.command.model.UpdateTenantGameCommand;
import java.util.UUID;

public interface TenantGameWritePort {
  boolean updateByGameId(UUID gameId, UpdateTenantGameCommand cmd);
}
