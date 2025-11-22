package com.tchalanet.server.tenant.domain.usecase;

import com.tchalanet.server.tenant.domain.model.TenantId;
import com.tchalanet.server.tenant.web.dto.ThemeCreateDto;
import com.tchalanet.server.tenant.web.dto.ThemeDto;
import com.tchalanet.server.tenant.web.dto.ThemeUpdateDto;
import java.util.List;
import java.util.UUID;

/**
 * Use case pour gérer les thèmes d'un tenant. Version initiale : facade/bridge vers le service
 * technique existant. TODO: migrer progressivement la logique métier ici, en utilisant des ports
 * domaine.
 */
public interface ConfigureTenantThemeUseCase {

  /** Liste les thèmes disponibles pour un tenant (base + tenant-specific). */
  List<ThemeDto> listThemes(TenantId tenantId, boolean includeBase);

  /** Récupère un thème par son ID (avec vérification tenant). */
  ThemeDto getTheme(TenantId tenantId, UUID themeId);

  /** Crée un nouveau thème pour le tenant. */
  ThemeDto createTheme(TenantId tenantId, ThemeCreateDto request);

  /** Met à jour un thème existant. */
  ThemeDto updateTheme(
      TenantId tenantId, UUID themeId, ThemeUpdateDto request, int expectedVersion);

  /** Publie un thème (DRAFT → PUBLISHED). */
  ThemeDto publishTheme(TenantId tenantId, UUID themeId);

  /** Archive un thème (PUBLISHED → ARCHIVED). */
  ThemeDto archiveTheme(TenantId tenantId, UUID themeId);
}
