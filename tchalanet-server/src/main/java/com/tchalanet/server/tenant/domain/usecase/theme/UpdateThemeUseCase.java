package com.tchalanet.server.tenant.domain.usecase.theme;

import com.tchalanet.server.tenant.infra.persistence.JpaThemeRepository;
import com.tchalanet.server.tenant.web.dto.ThemeDto;
import com.tchalanet.server.tenant.web.dto.ThemeUpdateDto;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Use case pour mettre à jour un thème existant. */
@Service
@RequiredArgsConstructor
public class UpdateThemeUseCase {

  private final JpaThemeRepository themeRepository;

  public ThemeDto execute(
      UUID tenantId, UUID themeId, ThemeUpdateDto dto, Integer expectedVersion) {
    var theme =
        themeRepository
            .findById(themeId)
            .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + themeId));

    // Vérifier l'accès
    if (theme.getTenantId() == null || !theme.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("Forbidden: Theme does not belong to tenant");
    }

    // Vérifier la version pour l'optimistic locking
    if (!Objects.equals(theme.getThemeVersion(), expectedVersion)) {
      throw new OptimisticLockingFailureException("Version mismatch");
    }

    // Mettre à jour les champs
    if (dto.label() != null) theme.setLabel(dto.label());
    if (dto.mode() != null) theme.setMode(dto.mode());
    if (dto.density() != null) theme.setDensity(dto.density());
    if (dto.palette() != null) theme.getPalette().putAll(dto.palette());
    if (dto.tokens() != null) theme.getTokens().putAll(dto.tokens());
    if (dto.cssVars() != null) theme.getCssVars().putAll(dto.cssVars());

    theme.setThemeVersion(theme.getThemeVersion() + 1);

    var saved = themeRepository.save(theme);

    return new ThemeDto(
        saved.getId(),
        saved.getTenantId(),
        saved.getBasePresetId(),
        saved.getLabel(),
        saved.getMode(),
        saved.getDensity(),
        saved.getPalette(),
        saved.getTokens(),
        saved.getCssVars(),
        saved.getStatus(),
        saved.getThemeVersion(),
        saved.getCreatedAt() == null
            ? null
            : OffsetDateTime.ofInstant(saved.getCreatedAt(), ZoneOffset.UTC),
        saved.getUpdatedAt() == null
            ? null
            : OffsetDateTime.ofInstant(saved.getUpdatedAt(), ZoneOffset.UTC));
  }
}
