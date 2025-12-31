package com.tchalanet.server.core.game.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.game.application.command.model.EnsureTenantGamesCommand;
import com.tchalanet.server.core.game.application.service.TenantGameEnsureService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EnsureTenantGamesCommandHandler
    implements CommandHandler<EnsureTenantGamesCommand, TenantGameEnsureService.EnsureResult> {

  private final TenantGameEnsureService service;

  @Override
  public TenantGameEnsureService.EnsureResult handle(EnsureTenantGamesCommand command) {
    return service.ensureByGameCodes(command.codes());
  }
}
