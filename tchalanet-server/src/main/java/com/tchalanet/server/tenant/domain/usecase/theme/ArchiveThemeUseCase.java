package com.tchalanet.server.tenant.domain.usecase.theme;

import com.tchalanet.server.tenant.domain.model.ThemeStatus;
import com.tchalanet.server.tenant.domain.usecase.EvictTenantThemeCacheUseCase;
import com.tchalanet.server.tenant.infra.persistence.JpaThemeRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Use case pour archiver un thème. Invalide le cache après archivage. */
@Service
@RequiredArgsConstructor
public class ArchiveThemeUseCase {

  private final JpaThemeRepository themeRepository;
  private final EvictTenantThemeCacheUseCase evictTenantThemeCacheUseCase;

  public void execute(UUID tenantId, UUID themeId) {
    var theme =
        themeRepository
            .findById(themeId)
            .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + themeId));

    // Vérifier l'accès
    if (theme.getTenantId() == null || !theme.getTenantId().equals(tenantId)) {
      throw new AccessDeniedException("Forbidden: Theme does not belong to tenant");
    }

    // Archiver le thème
    theme.setStatus(ThemeStatus.ARCHIVED);
    theme.setVersion(theme.getVersion() + 1);

    themeRepository.save(theme);

    // Invalider le cache du thème
    evictTenantThemeCacheUseCase.execute(tenantId);
  }
}
