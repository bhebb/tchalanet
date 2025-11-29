package com.tchalanet.server.core.tenant.domain.usecase.theme;

import com.tchalanet.server.core.tenant.domain.model.ThemeStatus;
import com.tchalanet.server.core.tenant.infra.persistence.JpaThemeRepository;
import com.tchalanet.server.core.tenant.infra.persistence.ThemeJpaEntity;
import com.tchalanet.server.core.tenant.web.dto.ThemeDto;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/** Use case pour lister les thèmes d'un tenant. */
@Service
@RequiredArgsConstructor
public class ListThemesUseCase {

  private final JpaThemeRepository themeRepository;

  public List<ThemeDto> execute(UUID tenantId, boolean includeBase, @Nullable ThemeStatus status) {
    var themes = new ArrayList<ThemeJpaEntity>();

    // Ajouter les thèmes de base si demandé
    if (includeBase) {
      themes.addAll(themeRepository.findByTenantIdIsNull());
    }

    // Ajouter les thèmes du tenant
    themes.addAll(themeRepository.findByTenantId(tenantId));

    // Filtrer par status si spécifié
    if (status != null) {
      themes.removeIf(t -> t.getStatus() != status);
    }

    return themes.stream().map(this::toDto).toList();
  }

  private ThemeDto toDto(ThemeJpaEntity entity) {
    return new ThemeDto(
        entity.getId(),
        entity.getTenantId(),
        entity.getBasePresetId(),
        entity.getLabel(),
        entity.getMode(),
        entity.getDensity(),
        entity.getPalette(),
        entity.getTokens(),
        entity.getCssVars(),
        entity.getStatus(),
        entity.getThemeVersion(),
        entity.getCreatedAt() == null
            ? null
            : OffsetDateTime.ofInstant(entity.getCreatedAt(), ZoneOffset.UTC),
        entity.getUpdatedAt() == null
            ? null
            : OffsetDateTime.ofInstant(entity.getUpdatedAt(), ZoneOffset.UTC));
  }
}
