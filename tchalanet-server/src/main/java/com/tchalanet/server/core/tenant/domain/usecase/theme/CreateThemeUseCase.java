package com.tchalanet.server.core.tenant.domain.usecase.theme;

import com.tchalanet.server.core.tenant.domain.model.ThemeMode;
import com.tchalanet.server.core.tenant.domain.model.ThemeStatus;
import com.tchalanet.server.core.tenant.infra.persistence.JpaThemeRepository;
import com.tchalanet.server.core.tenant.infra.persistence.ThemeJpaEntity;
import com.tchalanet.server.core.tenant.web.dto.ThemeCreateDto;
import com.tchalanet.server.core.tenant.web.dto.ThemeDto;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Use case pour créer un nouveau thème. */
@Service
@RequiredArgsConstructor
public class CreateThemeUseCase {

  private final JpaThemeRepository themeRepository;

  public ThemeDto execute(UUID tenantId, ThemeCreateDto dto) {
    var theme = new ThemeJpaEntity();
    theme.setId(UUID.randomUUID());
    theme.setTenantId(tenantId);
    theme.setBasePresetId(dto.basePresetId());
    theme.setLabel(dto.label());
    theme.setMode(Optional.ofNullable(dto.mode()).orElse(ThemeMode.SYSTEM));
    theme.setDensity(Optional.ofNullable(dto.density()).orElse((short) 0));
    theme.setPalette(Optional.ofNullable(dto.palette()).orElseGet(HashMap::new));
    theme.setTokens(Optional.ofNullable(dto.tokens()).orElseGet(HashMap::new));
    theme.setCssVars(Optional.ofNullable(dto.cssVars()).orElseGet(HashMap::new));
    theme.setStatus(ThemeStatus.DRAFT);
    theme.setThemeVersion(1);

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
