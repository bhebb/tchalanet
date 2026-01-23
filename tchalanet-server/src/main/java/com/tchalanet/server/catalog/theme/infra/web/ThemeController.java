package com.tchalanet.server.catalog.theme.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.theme.application.command.model.ArchiveThemeCommand;
import com.tchalanet.server.catalog.theme.application.command.model.PublishThemeCommand;
import com.tchalanet.server.catalog.theme.application.query.model.GetThemeByIdQuery;
import com.tchalanet.server.catalog.theme.application.query.model.ListThemesQuery;
import com.tchalanet.server.catalog.theme.application.query.model.ThemeView;
import com.tchalanet.server.catalog.theme.domain.model.ThemeStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API pour gérer les thèmes d'un tenant. */
@RestController
@RequestMapping("/admin/themes")
@RequiredArgsConstructor
@Tags({@Tag(name = "Admin • Themes"), @Tag(name = "Tenant • Themes")})
public class ThemeController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @Operation(summary = "List themes for tenant (admin)")
  @GetMapping
  @PreAuthorize("hasAuthority('TENANT_READ')")
  public ResponseEntity<List<ThemeView>> listThemes(
      @CurrentContext TchRequestContext context,
      @RequestParam(defaultValue = "false") boolean includeBase,
      @RequestParam(required = false) ThemeStatus status) {

    var tenantId = context.tenantid();
    var effectiveStatus = status != null ? status : ThemeStatus.PUBLISHED;

    var views = queryBus.send(new ListThemesQuery(tenantId, effectiveStatus, includeBase));

    return ResponseEntity.ok(views);
  }

  @Operation(summary = "Get theme by id (tenant)")
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('TENANT_READ')")
  public ResponseEntity<ThemeView> getTheme(
      @CurrentContext TchRequestContext context, @PathVariable UUID id) {

    var tenantId = context.tenantid();
    var theme = queryBus.send(new GetThemeByIdQuery(tenantId, id));

    return ResponseEntity.ok(theme);
  }

  @Operation(summary = "Publish theme version (tenant)")
  @PostMapping("/{id}/publish")
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ResponseEntity<Void> publishTheme(
      @CurrentContext TchRequestContext context,
      @PathVariable UUID id,
      @RequestParam Integer version) {

    TenantId tenantId = context.tenantid();

    commandBus.send(new PublishThemeCommand(tenantId, id, version));

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Archive theme (tenant)")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ResponseEntity<Void> archiveTheme(
      @CurrentContext TchRequestContext context, @PathVariable UUID id) {

    TenantId tenantId = context.tenantid();
    commandBus.send(new ArchiveThemeCommand(tenantId, id));

    return ResponseEntity.noContent().build();
  }
}
