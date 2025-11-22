package com.tchalanet.server.tenant.domain.usecase.theme;

import com.tchalanet.server.tenant.domain.model.ThemeStatus;
import com.tchalanet.server.tenant.domain.usecase.EvictTenantThemeCacheUseCase;
import com.tchalanet.server.tenant.infra.persistence.JpaThemeRepository;
import com.tchalanet.server.tenant.web.dto.ThemeDto;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Use case pour publier un thème. Invalide le cache après publication. */
@Service
@RequiredArgsConstructor
public class PublishThemeUseCase {

  private final JpaThemeRepository themeRepository;
  private final EvictTenantThemeCacheUseCase evictTenantThemeCacheUseCase;

  public ThemeDto execute(UUID tenantId, UUID themeId, Integer expectedVersion) {
    var theme =
        themeRepository
            .findById(themeId)
            .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + themeId));

    // Vérifier l'accès
    if (theme.getTenantId() == null || !theme.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("Forbidden: Theme does not belong to tenant");
    }

    // Vérifier la version pour l'optimistic locking
    if (!Objects.equals(theme.getVersion(), expectedVersion)) {
      throw new OptimisticLockingFailureException("Version mismatch");
    }

    // Publier le thème
    theme.setStatus(ThemeStatus.PUBLISHED);
    theme.setVersion(theme.getVersion() + 1);

    var saved = themeRepository.save(theme);

    // Invalider le cache du thème publié
    evictTenantThemeCacheUseCase.execute(tenantId);

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
        saved.getVersion(),
        saved.getCreatedAt(),
        saved.getUpdatedAt());
  }
}
