package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.ResolveTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.TenantGameDisabledEvent;
import com.tchalanet.server.platform.tenantgame.api.model.TenantGameEnabledEvent;
import com.tchalanet.server.platform.tenantgame.api.model.TenantGamePolicyUpdatedEvent;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGamePolicyRequest;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGamePersistenceAdapter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantGameService {

  private final GameCatalog gameCatalog;
  private final TenantGamePersistenceAdapter persistenceAdapter;
  private final ApplicationEventPublisher eventPublisher;
  private final JsonUtils jsonUtils;

  @TchTx
  public EnableTenantGameResult enableTenantGame(EnableTenantGameRequest request) {
    var gameView =
        gameCatalog
            .findByCode(request.getGameCode())
            .orElseThrow(
                () -> new IllegalArgumentException("Game not found: " + request.getGameCode()));

    var existing =
        persistenceAdapter.findByTenantIdAndGameCode(request.getTenantId(), request.getGameCode());

    var tenantGame =
        existing
            .map(current -> enableExisting(current, gameView))
            .orElseGet(() -> createTenantGame(request, gameView));

    var saved = persistenceAdapter.save(tenantGame);

    AfterCommit.run(
        () ->
            eventPublisher.publishEvent(
                new TenantGameEnabledEvent(
                    saved.tenantGameId(), saved.tenantId(), saved.code(), Instant.now(), "system")));

    return new EnableTenantGameResult(
        saved.tenantGameId(), saved.gameId(), saved.code(), saved.enabled());
  }

  @Transactional
  public DisableTenantGameResult disableTenantGame(DisableTenantGameRequest request) {
    var existing =
        persistenceAdapter
            .findByTenantIdAndGameCode(request.getTenantId(), request.getGameCode())
            .orElseThrow(() -> new IllegalArgumentException("Tenant game not found"));

    var saved = persistenceAdapter.save(copyWithEnabled(existing, false));

    AfterCommit.run(
        () ->
            eventPublisher.publishEvent(
                new TenantGameDisabledEvent(
                    saved.tenantGameId(), saved.tenantId(), saved.code(), Instant.now(), "system")));

    return new DisableTenantGameResult(saved.tenantGameId());
  }

  @Transactional
  public void ensureTenantGame(EnsureTenantGamesRequest request) {
    var game =
        gameCatalog
            .findByCode(request.getGameCode())
            .orElseThrow(
                () -> new IllegalArgumentException("Game not found: " + request.getGameCode()));

    if (persistenceAdapter.findByTenantIdAndGameCode(request.getTenantId(), request.getGameCode()).isPresent()) {
      return;
    }

    persistenceAdapter.save(
        new TenantGame(
            null,
            request.getTenantId(),
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
            jsonUtils.emptyObjectNode()));
  }

  @Transactional(readOnly = true)
  public List<TenantGame> resolveTenantGames(ResolveTenantGamesRequest request) {
    return persistenceAdapter.findAllByTenantId(request.getTenantId());
  }

  @Transactional
  public void updateTenantGamePolicy(UpdateTenantGamePolicyRequest request) {
    var existing =
        persistenceAdapter
            .findByTenantIdAndGameCode(request.getTenantId(), request.getGameCode())
            .orElseThrow(() -> new IllegalArgumentException("Tenant game not found"));

    var saved =
        persistenceAdapter.save(
            new TenantGame(
                existing.tenantGameId(),
                existing.tenantId(),
                existing.gameId(),
                existing.code(),
                existing.name(),
                existing.category(),
                existing.minDigits(),
                existing.maxDigits(),
                existing.combination(),
                existing.enabled(),
                existing.displayName(),
                existing.minStake(),
                existing.maxStake(),
                request.getPolicy()));

    AfterCommit.run(
        () ->
            eventPublisher.publishEvent(
                new TenantGamePolicyUpdatedEvent(
                    saved.tenantGameId(),
                    saved.tenantId(),
                    saved.code(),
                    Map.of(),
                    Instant.now(),
                    "system")));
  }

  private TenantGame enableExisting(TenantGame current, GameView gameView) {
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
        true,
        current.displayName(),
        current.minStake(),
        current.maxStake(),
        current.flags());
  }

  private TenantGame createTenantGame(EnableTenantGameRequest request, GameView gameView) {
    return new TenantGame(
        null,
        request.getTenantId(),
        gameView.id(),
        gameView.code(),
        gameView.name(),
        gameView.category(),
        gameView.minDigits(),
        gameView.maxDigits(),
        gameView.combination(),
        true,
        null,
        null,
        null,
        request.getPolicy());
  }

  private TenantGame copyWithEnabled(TenantGame current, boolean enabled) {
    return new TenantGame(
        current.tenantGameId(),
        current.tenantId(),
        current.gameId(),
        current.code(),
        current.name(),
        current.category(),
        current.minDigits(),
        current.maxDigits(),
        current.combination(),
        enabled,
        current.displayName(),
        current.minStake(),
        current.maxStake(),
        current.flags());
  }
}
