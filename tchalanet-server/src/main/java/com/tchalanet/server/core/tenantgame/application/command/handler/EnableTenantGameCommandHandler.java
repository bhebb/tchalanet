package com.tchalanet.server.core.tenantgame.application.command.handler;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.GameView;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.tenantgame.application.command.model.EnableTenantGameCommand;
import com.tchalanet.server.core.tenantgame.application.event.TenantGameEnabledEvent;
import com.tchalanet.server.core.tenantgame.application.command.model.EnableTenantGameCommandResult;
import com.tchalanet.server.core.tenantgame.application.port.TenantGamePersistencePort;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

/**
 * Handler for EnableTenantGameCommand.
 * Maps to spec requirement TG1 (enable/disable commands).
 * Uses @UseCase + @TchTx per command_query_handlers.md.
 * Validates game via GameCatalog API only (spec TG5 boundary).
 * Publishes event after commit (per event_model.md).
 */
@UseCase
@RequiredArgsConstructor
public class EnableTenantGameCommandHandler {

  private final GameCatalog gameCatalog;
  private final TenantGamePersistencePort persistencePort;
  private final ApplicationEventPublisher eventPublisher;

  @TchTx
  public EnableTenantGameCommandResult handle(EnableTenantGameCommand command) {
    // Validate game exists via GameCatalog (spec TG5: API only)
    var gameView = gameCatalog.findByCode(command.getGameCode())
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + command.getGameCode()));

    // Read existing tenant game if any
    var existing = persistencePort.findByTenantIdAndGameCode(command.getTenantId(), command.getGameCode());

    // Build domain model with all game metadata
    var tenantGame = existing.isPresent()
        ? updateExistingTenantGame(existing.get(), gameView)
        : createNewTenantGame(command, gameView);

    // Persist
    var saved = persistencePort.save(tenantGame);

    // Publish event after commit
    AfterCommit.run(() -> eventPublisher.publishEvent(
        new TenantGameEnabledEvent(
            saved.tenantGameId(),
            saved.tenantId(),
            saved.code(),
            Instant.now(),
            "system"
        )
    ));

    return new EnableTenantGameCommandResult(
        saved.tenantGameId(),
        saved.gameId(),
        saved.code(),
        saved.enabled()
    );
  }

  private TenantGame updateExistingTenantGame(TenantGame current, GameView gameView) {
    return new TenantGame(
        current.tenantGameId(),
        current.tenantId(),
        gameView.id(),
        gameView.code(),
        gameView.name(),
        gameView.category(),
        gameView.minDigits(),
        gameView.maxDigits(),
        gameView.combination(),
        true, // enabled
        current.displayName(),
        current.minStake(),
        current.maxStake(),
        current.flags()
    );
  }

  private TenantGame createNewTenantGame(EnableTenantGameCommand command, GameView gameView) {
    return new TenantGame(
        null, // tenantGameId will be generated on save
        command.getTenantId(),
        gameView.id(),
        gameView.code(),
        gameView.name(),
        gameView.category(),
        gameView.minDigits(),
        gameView.maxDigits(),
        gameView.combination(),
        true, // enabled
        null, // displayName (tenant can override)
        null, // minStake (tenant can customize)
        null, // maxStake (tenant can customize)
        null  // flags (tenant can configure)
    );
  }
}
