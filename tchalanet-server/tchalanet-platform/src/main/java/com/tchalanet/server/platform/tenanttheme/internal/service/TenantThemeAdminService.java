package com.tchalanet.server.platform.tenanttheme.internal.service;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.DeactivateTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeAdminView;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeUpdatedEvent;
import com.tchalanet.server.platform.tenanttheme.internal.persistence.TenantThemePersistenceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TenantThemeAdminService {

    private final ThemeCatalog themeCatalog;
    private final TenantThemePersistenceAdapter persistence;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public void applyPreset(ApplyTenantThemeRequest request) {
        var preset = themeCatalog.findByCode(request.presetCode())
            .orElseThrow(() -> new IllegalArgumentException("Theme preset not found: " + request.presetCode()));
        if (!preset.active()) {
            throw new IllegalArgumentException("Theme preset is not active: " + request.presetCode());
        }

        var existing = persistence.findByTenantId(request.tenantId());
        long newVersion = existing.map(t -> t.version() + 1).orElse(1L);
        Instant now = Instant.now(clock);

        var saved = persistence.save(new TenantTheme(
            request.tenantId(), request.presetCode(), "SYSTEM",
            true, false, newVersion,
            existing.map(TenantTheme::createdAt).orElse(now), now, "system"));

        AfterCommit.run(() -> eventPublisher.publishEvent(
            new TenantThemeUpdatedEvent(saved.tenantId(), saved.presetCode(), saved.version(), saved.updatedAt(), "system")));
    }

    @Transactional
    public void deactivate(DeactivateTenantThemeRequest request) {
        var existing = persistence.findByTenantId(request.tenantId());
        if (existing.isEmpty()) return;
        persistence.deactivate(request.tenantId());
        var theme = existing.get();
        AfterCommit.run(() -> eventPublisher.publishEvent(
            new TenantThemeUpdatedEvent(theme.tenantId(), null, theme.version() + 1, Instant.now(clock), "system")));
    }

    @Transactional(readOnly = true)
    public TenantThemeAdminView getAdminView(TenantId tenantId) {
        return persistence.findActiveByTenantId(tenantId)
            .map(t -> new TenantThemeAdminView(
                t.presetCode(), t.defaultMode(), t.active(), t.isDefault(), t.version(), t.updatedAt()))
            .orElse(null);
    }
}
