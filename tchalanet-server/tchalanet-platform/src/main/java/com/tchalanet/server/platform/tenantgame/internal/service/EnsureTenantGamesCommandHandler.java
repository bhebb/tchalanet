package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.tenantgame.application.command.model.EnsureTenantGamesCommand;
import com.tchalanet.server.core.tenantgame.application.port.TenantGamePersistencePort;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EnsureTenantGamesCommandHandler {

  private final GameCatalog gameCatalog;
  private final TenantGamePersistencePort persistencePort;
  private final JsonUtils jsonUtils;

  @Transactional
  public void handle(EnsureTenantGamesCommand command) {
    var game = gameCatalog.findByCode(command.getGameCode())
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + command.getGameCode()));

    var existing = persistencePort.findByTenantIdAndGameCode(command.getTenantId(), command.getGameCode());
    if (existing.isPresent()) {
      return; // Idempotent
    }

    var tenantGame = new TenantGame(
        null,
        command.getTenantId(),
        game.id(),
        game.code(),
        game.name(),
        game.category(),
        game.minDigits(),
        game.maxDigits(),
        game.combination(),
        true,
        game.name(),
        null,
        null,
        jsonUtils.emptyObjectNode()
     );

    persistencePort.save(tenantGame);
  }
}
