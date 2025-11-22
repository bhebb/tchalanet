package com.tchalanet.server.tenant.domain.usecase.theme;

import com.tchalanet.server.tenant.infra.persistence.JpaThemeRepository;
import com.tchalanet.server.tenant.web.dto.ThemeDto;
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
        theme.getVersion(),
        theme.getCreatedAt(),
        theme.getUpdatedAt());
  }
}
