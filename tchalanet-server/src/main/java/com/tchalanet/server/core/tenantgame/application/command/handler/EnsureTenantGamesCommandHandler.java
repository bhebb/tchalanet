package com.tchalanet.server.core.tenantgame.application.command.handler;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.core.tenantgame.application.command.model.EnsureTenantGamesCommand;
import com.tchalanet.server.core.tenantgame.application.port.TenantGamePersistencePort;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EnsureTenantGamesCommandHandler {

  private final GameCatalog gameCatalog;
  private final TenantGamePersistencePort persistencePort;

  @Transactional
  public void handle(EnsureTenantGamesCommand command) {
    var game = gameCatalog.findByCode(command.getGameCode())
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + command.getGameCode()));

    var existing = persistencePort.findByTenantIdAndGameCode(command.getTenantId(), command.getGameCode());
    if (existing.isPresent()) {
      return; // Idempotent
    }

    var tenantGame = TenantGame.builder()
        .tenantId(command.getTenantId())
        .gameCode(command.getGameCode())
        .enabled(true) // Default enabled
        .build();

    persistencePort.save(tenantGame);
  }
}
