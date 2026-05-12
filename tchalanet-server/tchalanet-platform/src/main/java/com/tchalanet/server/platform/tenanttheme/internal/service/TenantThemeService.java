package com.tchalanet.server.platform.tenanttheme.internal.service;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeCommand;
import com.tchalanet.server.platform.tenanttheme.api.model.DeactivateTenantThemeCommand;
import com.tchalanet.server.platform.tenanttheme.api.model.ResolveTenantThemeQuery;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeNotice;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeUpdatedEvent;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeView;
import com.tchalanet.server.platform.tenanttheme.internal.persistence.TenantThemePersistenceAdapter;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantThemeService {

  private static final Logger logger = LoggerFactory.getLogger(TenantThemeService.class);
  private static final Marker FALLBACK_MARKER = MarkerFactory.getMarker("TENANT_THEME_FALLBACK");

  private final ThemeCatalog themeCatalog;
  private final TenantThemePersistenceAdapter persistenceAdapter;
  private final TenantThemeFallbackService fallbackService;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @TchTx
  public void applyTenantTheme(ApplyTenantThemeCommand request) {
    var preset =
        themeCatalog
            .findByCode(request.presetCode())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Theme preset not found or inactive: " + request.presetCode()));

    if (!preset.active()) {
      throw new IllegalArgumentException("Theme preset is not active: " + request.presetCode());
    }

    var existing = persistenceAdapter.findByTenantId(request.tenantId());
    long newVersion = existing.map(t -> t.version() + 1).orElse(1L);
    Instant now = Instant.now(clock);

    var saved =
        persistenceAdapter.save(
            new TenantTheme(
                request.tenantId(),
                request.presetCode(),
                new HashMap<>(),
                false,
                newVersion,
                existing.map(TenantTheme::createdAt).orElse(now),
                now,
                "system"));

    AfterCommit.run(
        () ->
            eventPublisher.publishEvent(
                new TenantThemeUpdatedEvent(
                    saved.tenantId(),
                    saved.presetCode(),
                    saved.version(),
                    saved.updatedAt(),
                    saved.createdBy())));
  }

  @TchTx
  public void deactivateTenantTheme(DeactivateTenantThemeCommand request) {
    var existing = persistenceAdapter.findByTenantId(request.tenantId());
    if (existing.isEmpty()) {
      return;
    }

    persistenceAdapter.deactivate(request.tenantId());
    var theme = existing.get();

    AfterCommit.run(
        () ->
            eventPublisher.publishEvent(
                new TenantThemeUpdatedEvent(
                    theme.tenantId(), null, theme.version() + 1, Instant.now(clock), "system")));
  }

  @Transactional(readOnly = true)
  public TenantThemeView resolveTenantTheme(ResolveTenantThemeQuery request) {
    var tenantTheme = persistenceAdapter.findByTenantId(request.tenantId());
    if (tenantTheme.isEmpty()) {
      return applyFallback(request.tenantId(), null);
    }

    var requestedCode = tenantTheme.get().presetCode();
    var preset = themeCatalog.findByCode(requestedCode);
    if (preset.isEmpty() || !preset.get().active()) {
      return applyFallback(request.tenantId(), requestedCode);
    }

    var theme = tenantTheme.get();
    return new TenantThemeView(
        theme.tenantId(),
        theme.presetCode(),
        theme.metadata(),
        theme.isDefault(),
        theme.version(),
        theme.updatedAt());
  }

  private TenantThemeView applyFallback(TenantId tenantId, String requestedCode) {
    var fallbackCode = fallbackService.resolveFallback(tenantId, requestedCode);
    emitNotice(tenantId, requestedCode, fallbackCode);
    return new TenantThemeView(tenantId, fallbackCode, Map.of(), false, 0L, Instant.now(clock));
  }

  private void emitNotice(TenantId tenantId, String requestedCode, String fallbackCode) {
    var notice =
        TenantThemeNotice.fallbackApplied(tenantId, requestedCode, fallbackCode, Instant.now(clock));
    logger.warn(
        FALLBACK_MARKER,
        "Theme preset unavailable, fallback applied: tenantId={}, requestedPresetCode={}, fallbackPresetCode={}, timestamp={}",
        notice.tenantId(),
        notice.requestedPresetCode(),
        notice.fallbackPresetCode(),
        notice.timestamp());
  }
}
