package com.tchalanet.server.core.tenant.domain.usecase;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import com.tchalanet.server.core.tenant.domain.usecase.theme.ArchiveThemeUseCase;
import com.tchalanet.server.core.tenant.domain.usecase.theme.CreateThemeUseCase;
import com.tchalanet.server.core.tenant.domain.usecase.theme.GetThemeUseCase;
import com.tchalanet.server.core.tenant.domain.usecase.theme.ListThemesUseCase;
import com.tchalanet.server.core.tenant.domain.usecase.theme.PublishThemeUseCase;
import com.tchalanet.server.core.tenant.domain.usecase.theme.UpdateThemeUseCase;
import com.tchalanet.server.core.tenant.web.dto.ThemeCreateDto;
import com.tchalanet.server.core.tenant.web.dto.ThemeDto;
import com.tchalanet.server.core.tenant.web.dto.ThemeUpdateDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Implémentation du use case de gestion des thèmes tenant. Orchestre les use cases spécialisés pour
 * chaque opération.
 */
@UseCase
@RequiredArgsConstructor
public class ConfigureTenantThemeUseCaseImpl implements ConfigureTenantThemeUseCase {

  private final ListThemesUseCase listThemesUseCase;
  private final GetThemeUseCase getThemeUseCase;
  private final CreateThemeUseCase createThemeUseCase;
  private final UpdateThemeUseCase updateThemeUseCase;
  private final PublishThemeUseCase publishThemeUseCase;
  private final ArchiveThemeUseCase archiveThemeUseCase;

  @Override
  public List<ThemeDto> listThemes(TenantId tenantId, boolean includeBase) {
    return listThemesUseCase.execute(tenantId.value(), includeBase, null);
  }

  @Override
  public ThemeDto getTheme(TenantId tenantId, UUID themeId) {
    return getThemeUseCase.execute(tenantId.value(), themeId);
  }

  @Override
  public ThemeDto createTheme(TenantId tenantId, ThemeCreateDto request) {
    return createThemeUseCase.execute(tenantId.value(), request);
  }

  @Override
  public ThemeDto updateTheme(
      TenantId tenantId, UUID themeId, ThemeUpdateDto request, int expectedVersion) {
    return updateThemeUseCase.execute(tenantId.value(), themeId, request, expectedVersion);
  }

  @Override
  public ThemeDto publishTheme(TenantId tenantId, UUID themeId) {
    // TODO: version devrait être passée explicitement
    return publishThemeUseCase.execute(tenantId.value(), themeId, 0);
  }

  @Override
  public ThemeDto archiveTheme(TenantId tenantId, UUID themeId) {
    archiveThemeUseCase.execute(tenantId.value(), themeId);
    return null;
  }
}
