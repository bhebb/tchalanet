package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.TenantGameDisabledEvent;
import com.tchalanet.server.platform.tenantgame.api.model.TenantGameEnabledEvent;
import com.tchalanet.server.platform.tenantgame.api.model.TenantGameSettingsUpdatedEvent;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameBetOptionConfigRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameBetOptionConfigView;
import com.tchalanet.server.platform.tenantgame.internal.cache.TenantGameCacheNames;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGamePersistenceAdapter;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantGameAdminService {

  private final GameCatalog gameCatalog;
  private final TenantGamePersistenceAdapter persistence;
  private final TenantGameConfigValidator validator;
  private final TenantGameBetOptionConfigs betOptionConfigs;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  @CacheEvict(
      cacheNames = {TenantGameCacheNames.RUNTIME, TenantGameCacheNames.PROJECTION},
      allEntries = true)
  public EnableTenantGameResult enableGame(EnableTenantGameRequest request) {
    var game =
        gameCatalog
            .findByCode(request.getGameCode().toUpperCase())
            .orElseThrow(
                () -> new IllegalArgumentException("Game not found: " + request.getGameCode()));
    validator.validateEnableGame(game);

    var existing = persistence.findByTenantIdAndGameCode(request.getTenantId(), game.code());
    var tenantGame =
        existing
            .map(
                current ->
                    new TenantGame(
                        current.tenantGameId(),
                        current.tenantId(),
                        current.gameId(),
                        current.gameCode(),
                        true,
                        current.visibleInPos(),
                        current.displayName(),
                        current.displayOrder(),
                        current.minStake(),
                        current.maxStake(),
                        current.availabilityEnabled(),
                        current.availabilityDays(),
                        current.startLocalTime(),
                        current.endLocalTime(),
                        betOptionConfigs.effectiveFor(current)))
            .orElseGet(
                () ->
                    new TenantGame(
                        null,
                        request.getTenantId(),
                        game.id(),
                        game.code().toUpperCase(),
                        true,
                        true,
                        null,
                        0,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        betOptionConfigs.defaultsFor(game.code())));

    var saved = persistence.save(tenantGame);
    AfterCommit.run(
        () ->
            eventPublisher.publishEvent(
                new TenantGameEnabledEvent(
                    saved.tenantGameId(),
                    saved.tenantId(),
                    saved.gameCode(),
                    Instant.now(),
                    "system")));
    return new EnableTenantGameResult(
        saved.tenantGameId(),
        saved.gameId(),
        saved.gameCode(),
        saved.enabled(),
        saved.visibleInPos(),
        saved.displayName(),
        saved.displayOrder(),
        saved.minStake(),
        saved.maxStake());
  }

  @Transactional
  @CacheEvict(
      cacheNames = {TenantGameCacheNames.RUNTIME, TenantGameCacheNames.PROJECTION},
      allEntries = true)
  public DisableTenantGameResult disableGame(DisableTenantGameRequest request) {
    var existing =
        persistence
            .findByTenantIdAndGameCode(request.getTenantId(), request.getGameCode().toUpperCase())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Tenant game not found: " + request.getGameCode()));
    var updated =
        new TenantGame(
            existing.tenantGameId(),
            existing.tenantId(),
            existing.gameId(),
            existing.gameCode(),
            false,
            existing.visibleInPos(),
            existing.displayName(),
            existing.displayOrder(),
            existing.minStake(),
            existing.maxStake(),
            existing.availabilityEnabled(),
            existing.availabilityDays(),
            existing.startLocalTime(),
            existing.endLocalTime(),
            betOptionConfigs.effectiveFor(existing));
    var saved = persistence.save(updated);
    AfterCommit.run(
        () ->
            eventPublisher.publishEvent(
                new TenantGameDisabledEvent(
                    saved.tenantGameId(),
                    saved.tenantId(),
                    saved.gameCode(),
                    Instant.now(),
                    "system")));
    return new DisableTenantGameResult(saved.tenantGameId());
  }

  @Transactional
  @CacheEvict(
      cacheNames = {TenantGameCacheNames.RUNTIME, TenantGameCacheNames.PROJECTION},
      allEntries = true)
  public void updateSettings(UpdateTenantGameSettingsRequest request) {
    validator.validateSettings(request);
    var existing =
        persistence
            .findByTenantIdAndGameCode(request.getTenantId(), request.getGameCode().toUpperCase())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Tenant game not found: " + request.getGameCode()));

    var availabilityEnabled =
        request.getAvailabilityEnabled() != null
            ? request.getAvailabilityEnabled()
            : existing.availabilityEnabled();
    var effectiveMinStake =
        request.getMinStake() != null ? request.getMinStake() : existing.minStake();
    var effectiveMaxStake =
        request.getMaxStake() != null ? request.getMaxStake() : existing.maxStake();
    validator.validateStakeRange(effectiveMinStake, effectiveMaxStake);
    var updated =
        new TenantGame(
            existing.tenantGameId(),
            existing.tenantId(),
            existing.gameId(),
            existing.gameCode(),
            existing.enabled(),
            request.getVisibleInPos() != null ? request.getVisibleInPos() : existing.visibleInPos(),
            request.getDisplayName() != null
                ? blankToNull(request.getDisplayName())
                : existing.displayName(),
            request.getDisplayOrder() != null ? request.getDisplayOrder() : existing.displayOrder(),
            effectiveMinStake,
            effectiveMaxStake,
            availabilityEnabled,
            availabilityEnabled
                ? coalesceText(request.getAvailabilityDays(), existing.availabilityDays())
                : null,
            availabilityEnabled
                ? coalesceTime(request.getStartLocalTime(), existing.startLocalTime())
                : null,
            availabilityEnabled
                ? coalesceTime(request.getEndLocalTime(), existing.endLocalTime())
                : null,
            betOptionConfigs.effectiveFor(existing));

    var saved = persistence.save(updated);
    AfterCommit.run(
        () ->
            eventPublisher.publishEvent(
                new TenantGameSettingsUpdatedEvent(
                    saved.tenantGameId(),
                    saved.tenantId(),
                    saved.gameCode(),
                    Instant.now(),
                    "system")));
  }

  @Transactional(readOnly = true)
  public List<TenantGame> listGames(TenantId tenantId) {
    return persistence.findAllByTenantId(tenantId);
  }

  @Transactional(readOnly = true)
  public TenantGameBetOptionConfigView getBetOptionConfig(TenantId tenantId, String gameCode) {
    var existing =
        persistence
            .findByTenantIdAndGameCode(tenantId, gameCode.toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Tenant game not found: " + gameCode));
    return betOptionConfigs.toView(existing);
  }

  @Transactional
  @CacheEvict(
      cacheNames = {TenantGameCacheNames.RUNTIME, TenantGameCacheNames.PROJECTION},
      allEntries = true)
  public TenantGameBetOptionConfigView updateBetOptionConfig(
      UpdateTenantGameBetOptionConfigRequest request) {
    var gameCode = request.getGameCode().toUpperCase();
    var existing =
        persistence
            .findByTenantIdAndGameCode(request.getTenantId(), gameCode)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Tenant game not found: " + request.getGameCode()));
    List<com.tchalanet.server.platform.tenantgame.api.model.TenantBetTypeOptionConfig> normalized;
    try {
      normalized = betOptionConfigs.normalize(gameCode, request.getBetTypes());
    } catch (IllegalArgumentException ex) {
      throw ProblemRest.badRequest(ex.getMessage());
    }
    var updated =
        new TenantGame(
            existing.tenantGameId(),
            existing.tenantId(),
            existing.gameId(),
            existing.gameCode(),
            existing.enabled(),
            existing.visibleInPos(),
            existing.displayName(),
            existing.displayOrder(),
            existing.minStake(),
            existing.maxStake(),
            existing.availabilityEnabled(),
            existing.availabilityDays(),
            existing.startLocalTime(),
            existing.endLocalTime(),
            normalized);
    var saved = persistence.save(updated);
    AfterCommit.run(
        () ->
            eventPublisher.publishEvent(
                new TenantGameSettingsUpdatedEvent(
                    saved.tenantGameId(),
                    saved.tenantId(),
                    saved.gameCode(),
                    Instant.now(),
                    "system")));
    return betOptionConfigs.toView(saved);
  }

  private static LocalTime coalesceTime(String value, LocalTime fallback) {
    if (value == null) {
      return fallback;
    }
    if (value.isBlank()) {
      return null;
    }
    return LocalTime.parse(value.trim());
  }

  private static String coalesceText(String value, String fallback) {
    if (value == null) {
      return fallback;
    }
    return blankToNull(value);
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
