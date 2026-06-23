package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.TenantGameDisabledEvent;
import com.tchalanet.server.platform.tenantgame.api.model.TenantGameEnabledEvent;
import com.tchalanet.server.platform.tenantgame.api.model.TenantGameSettingsUpdatedEvent;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGamePersistenceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantGameAdminService {

    private final GameCatalog gameCatalog;
    private final TenantGamePersistenceAdapter persistence;
    private final TenantGameConfigValidator validator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public EnableTenantGameResult enableGame(EnableTenantGameRequest request) {
        var game = gameCatalog.findByCode(request.getGameCode().toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Game not found: " + request.getGameCode()));
        validator.validateEnableGame(game);

        var existing = persistence.findByTenantIdAndGameCode(request.getTenantId(), game.code());
        var tenantGame = existing
            .map(current -> new TenantGame(
                current.tenantGameId(), current.tenantId(), current.gameId(), current.gameCode(),
                true, current.visibleInPos(), current.displayName(), current.displayOrder(),
                current.minStake(), current.maxStake(), current.availabilityEnabled(),
                current.availabilityDays(), current.startLocalTime(), current.endLocalTime()))
            .orElseGet(() -> new TenantGame(
                null, request.getTenantId(), game.id(), game.code().toUpperCase(),
                true, true, null, 0, null, null, false, null, null, null));

        var saved = persistence.save(tenantGame);
        AfterCommit.run(() -> eventPublisher.publishEvent(
            new TenantGameEnabledEvent(saved.tenantGameId(), saved.tenantId(), saved.gameCode(), Instant.now(), "system")));
        return new EnableTenantGameResult(
            saved.tenantGameId(), saved.gameId(), saved.gameCode(), saved.enabled(),
            saved.visibleInPos(), saved.displayName(), saved.displayOrder(),
            saved.minStake(), saved.maxStake());
    }

    @Transactional
    public DisableTenantGameResult disableGame(DisableTenantGameRequest request) {
        var existing = persistence.findByTenantIdAndGameCode(request.getTenantId(), request.getGameCode().toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Tenant game not found: " + request.getGameCode()));
        var updated = new TenantGame(
            existing.tenantGameId(), existing.tenantId(), existing.gameId(), existing.gameCode(),
            false, existing.visibleInPos(), existing.displayName(), existing.displayOrder(),
            existing.minStake(), existing.maxStake(), existing.availabilityEnabled(),
            existing.availabilityDays(), existing.startLocalTime(), existing.endLocalTime());
        var saved = persistence.save(updated);
        AfterCommit.run(() -> eventPublisher.publishEvent(
            new TenantGameDisabledEvent(saved.tenantGameId(), saved.tenantId(), saved.gameCode(), Instant.now(), "system")));
        return new DisableTenantGameResult(saved.tenantGameId());
    }

    @Transactional
    public void updateSettings(UpdateTenantGameSettingsRequest request) {
        validator.validateSettings(request);
        var existing = persistence.findByTenantIdAndGameCode(request.getTenantId(), request.getGameCode().toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Tenant game not found: " + request.getGameCode()));

        var updated = new TenantGame(
            existing.tenantGameId(), existing.tenantId(), existing.gameId(), existing.gameCode(),
            existing.enabled(),
            request.getVisibleInPos() != null ? request.getVisibleInPos() : existing.visibleInPos(),
            request.getDisplayName() != null ? request.getDisplayName() : existing.displayName(),
            request.getDisplayOrder() != null ? request.getDisplayOrder() : existing.displayOrder(),
            request.getMinStake() != null ? request.getMinStake() : existing.minStake(),
            request.getMaxStake() != null ? request.getMaxStake() : existing.maxStake(),
            request.getAvailabilityEnabled() != null ? request.getAvailabilityEnabled() : existing.availabilityEnabled(),
            request.getAvailabilityDays() != null ? request.getAvailabilityDays() : existing.availabilityDays(),
            request.getStartLocalTime() != null ? LocalTime.parse(request.getStartLocalTime()) : existing.startLocalTime(),
            request.getEndLocalTime() != null ? LocalTime.parse(request.getEndLocalTime()) : existing.endLocalTime());

        var saved = persistence.save(updated);
        AfterCommit.run(() -> eventPublisher.publishEvent(
            new TenantGameSettingsUpdatedEvent(saved.tenantGameId(), saved.tenantId(), saved.gameCode(), Instant.now(), "system")));
    }

    @Transactional(readOnly = true)
    public List<TenantGame> listGames(TenantId tenantId) {
        return persistence.findAllByTenantId(tenantId);
    }
}
