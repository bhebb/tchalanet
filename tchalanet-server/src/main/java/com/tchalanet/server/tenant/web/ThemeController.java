package com.tchalanet.server.tenant.web;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.tenant.domain.model.TenantId;
import com.tchalanet.server.tenant.domain.model.ThemeStatus;
import com.tchalanet.server.tenant.domain.usecase.ConfigureTenantThemeUseCase;
import com.tchalanet.server.tenant.domain.usecase.theme.CreateThemeUseCase;
import com.tchalanet.server.tenant.domain.usecase.theme.GetThemeUseCase;
import com.tchalanet.server.tenant.domain.usecase.theme.ListThemesUseCase;
import com.tchalanet.server.tenant.domain.usecase.theme.PublishThemeUseCase;
import com.tchalanet.server.tenant.domain.usecase.theme.UpdateThemeUseCase;
import com.tchalanet.server.tenant.web.dto.ThemeCreateDto;
import com.tchalanet.server.tenant.web.dto.ThemeDto;
import com.tchalanet.server.tenant.web.dto.ThemeUpdateDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** REST API pour gérer les thèmes d'un tenant. */
@RestController
@RequestMapping("/api/v1/themes")
@RequiredArgsConstructor
public class ThemeController {

  private final ConfigureTenantThemeUseCase configureTenantThemeUseCase;
  private final ListThemesUseCase listThemesUseCase;
  private final CreateThemeUseCase createThemeUseCase;
  private final GetThemeUseCase getThemeUseCase;
  private final UpdateThemeUseCase updateThemeUseCase;
  private final PublishThemeUseCase publishThemeUseCase;

  @GetMapping
  @PreAuthorize("hasAuthority('TENANT_READ')")
  public ResponseEntity<List<ThemeDto>> listThemes(
      @CurrentContext TchRequestContext context,
      @RequestParam(defaultValue = "false") boolean includeBase,
      @RequestParam(required = false) ThemeStatus status) {

    var tenantId = new TenantId(context.effectiveTenant());
    List<ThemeDto> themes = configureTenantThemeUseCase.listThemes(tenantId, includeBase);

    // TODO: Ajouter le filtrage par status au use case si nécessaire
    return ResponseEntity.ok(themes);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('TENANT_READ')")
  public ResponseEntity<ThemeDto> getTheme(
      @CurrentContext TchRequestContext context, @PathVariable UUID id) {

    var tenantId = new TenantId(context.effectiveTenant());
    ThemeDto theme = configureTenantThemeUseCase.getTheme(tenantId, id);

    return ResponseEntity.ok(theme);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ResponseEntity<ThemeDto> createTheme(
      @CurrentContext TchRequestContext context, @Valid @RequestBody ThemeCreateDto dto) {

    var tenantId = new TenantId(context.effectiveTenant());
    ThemeDto theme = configureTenantThemeUseCase.createTheme(tenantId, dto);

    return ResponseEntity.ok(theme);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ResponseEntity<ThemeDto> updateTheme(
      @CurrentContext TchRequestContext context,
      @PathVariable UUID id,
      @Valid @RequestBody ThemeUpdateDto dto) {

    var tenantId = new TenantId(context.effectiveTenant());
    ThemeDto theme = configureTenantThemeUseCase.updateTheme(tenantId, id, dto, dto.version());

    return ResponseEntity.ok(theme);
  }

  @PostMapping("/{id}/publish")
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ResponseEntity<ThemeDto> publishTheme(
      @CurrentContext TchRequestContext context,
      @PathVariable UUID id,
      @RequestParam Integer version) {

    var tenantId = new TenantId(context.effectiveTenant());
    ThemeDto theme = configureTenantThemeUseCase.publishTheme(tenantId, id);

    // TODO: Le use case devrait vérifier la version pour éviter les conflits
    return ResponseEntity.ok(theme);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ResponseEntity<Void> archiveTheme(
      @CurrentContext TchRequestContext context, @PathVariable UUID id) {

    var tenantId = new TenantId(context.effectiveTenant());
    configureTenantThemeUseCase.archiveTheme(tenantId, id);

    return ResponseEntity.noContent().build();
  }
}
