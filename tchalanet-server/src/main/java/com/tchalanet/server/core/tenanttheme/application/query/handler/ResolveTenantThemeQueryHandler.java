package com.tchalanet.server.core.tenanttheme.application.query.handler;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenanttheme.application.model.TenantThemeNotice;
import com.tchalanet.server.core.tenanttheme.application.port.out.TenantThemeReaderPort;
import com.tchalanet.server.core.tenanttheme.application.query.model.ResolveTenantThemeQuery;
import com.tchalanet.server.core.tenanttheme.application.query.model.TenantThemeView;
import com.tchalanet.server.core.tenanttheme.application.service.TenantThemeFallbackService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Handler for ResolveTenantThemeQuery.
 * Maps to spec requirements T6 + DP3 (fallback resolution).
 */
@UseCase
@RequiredArgsConstructor
public class ResolveTenantThemeQueryHandler
    implements QueryHandler<ResolveTenantThemeQuery, TenantThemeView> {

  private static final Logger logger =
      LoggerFactory.getLogger(ResolveTenantThemeQueryHandler.class);
  private static final Marker FALLBACK_MARKER = MarkerFactory.getMarker("TENANT_THEME_FALLBACK");

  private final TenantThemeReaderPort readerPort;
  private final ThemeCatalog themeCatalog;
  private final TenantThemeFallbackService fallbackService;
  private final Clock clock;

  @Override
  public TenantThemeView handle(ResolveTenantThemeQuery query) {
    var tenantTheme = readerPort.findByTenantId(query.tenantId());

    // No theme configured for this tenant → fallback immediately
    if (tenantTheme.isEmpty()) {
      return applyFallback(query.tenantId(), null);
    }

    var requestedCode = tenantTheme.get().presetCode();
    var preset = themeCatalog.findByCode(requestedCode);

    // Preset unavailable (not found OR inactive) → fallback
    if (preset.isEmpty() || !preset.get().active()) {
      return applyFallback(query.tenantId(), requestedCode);
    }

    // Preset available → return as-is
    var t = tenantTheme.get();
    return new TenantThemeView(
        t.tenantId(), t.presetCode(), t.metadata(), t.isDefault(), t.version(), t.updatedAt());
  }

  private TenantThemeView applyFallback(
      com.tchalanet.server.common.types.id.TenantId tenantId, String requestedCode) {
    var fallbackCode = fallbackService.resolveFallback(tenantId, requestedCode);
    emitNotice(tenantId, requestedCode, fallbackCode);

    // Build view with fallback preset
    return new TenantThemeView(
        tenantId,
        fallbackCode,
        java.util.Map.of(), // empty metadata for fallback
        false, // isDefault = false for fallback
        0L, // version 0 for fallback
        Instant.now(clock));
  }

  private void emitNotice(
      com.tchalanet.server.common.types.id.TenantId tenantId,
      String requestedCode,
      String fallbackCode) {
    var notice =
        TenantThemeNotice.fallbackApplied(tenantId, requestedCode, fallbackCode, Instant.now(clock));

    // Structured logging (NF1)
    logger.warn(
        FALLBACK_MARKER,
        "Theme preset unavailable, fallback applied: tenantId={}, requestedPresetCode={}, fallbackPresetCode={}, timestamp={}",
        notice.tenantId(),
        notice.requestedPresetCode(),
        notice.fallbackPresetCode(),
        notice.timestamp());

    // TODO: Optional - publish as event via ApplicationEventPublisher if needed
  }
}
