package com.tchalanet.server.core.theme.application.query.handler;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Use case pour récupérer un thème spécifique. */
@Service
@RequiredArgsConstructor
public class GetThemeUseCase {

  private final JpaThemeRepository themeRepository;

  public ThemeDto execute(UUID tenantId, UUID themeId) {
    var theme =
        themeRepository
            .findById(themeId)
            .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + themeId));

    // Vérifier l'accès : le thème doit être public (tenantId null) ou appartenir au tenant
    if (theme.getTenantId() != null && !theme.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("Forbidden: Theme does not belong to tenant");
    }

    return new ThemeDto(
        theme.getId(),
        theme.getTenantId(),
        theme.getBasePresetId(),
        theme.getLabel(),
        theme.getMode(),
        theme.getDensity(),
        theme.getPalette(),
        theme.getTokens(),
        theme.getCssVars(),
        theme.getStatus(),
        theme.getThemeVersion(),
        theme.getCreatedAt() == null
            ? null
            : OffsetDateTime.ofInstant(theme.getCreatedAt(), ZoneOffset.UTC),
        theme.getUpdatedAt() == null
            ? null
            : OffsetDateTime.ofInstant(theme.getUpdatedAt(), ZoneOffset.UTC));
  }
}
