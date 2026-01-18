package com.tchalanet.server.catalog.game.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.catalog.game.application.command.model.UpdateTenantGameCommand;
import com.tchalanet.server.catalog.game.application.port.out.TenantGameWritePort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateTenantGameCommandHandler
    implements CommandHandler<UpdateTenantGameCommand, Boolean> {

  private final TenantGameWritePort writePort;

  @Override
  public Boolean handle(UpdateTenantGameCommand command) {
    return writePort.updateByGameId(command.gameId(), command);
  }
}
