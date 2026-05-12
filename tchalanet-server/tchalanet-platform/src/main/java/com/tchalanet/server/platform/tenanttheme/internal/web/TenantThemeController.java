package com.tchalanet.server.platform.tenanttheme.internal.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenanttheme.application.command.model.ApplyTenantThemeCommand;
import com.tchalanet.server.core.tenanttheme.application.command.model.DeactivateTenantThemeCommand;
import com.tchalanet.server.core.tenanttheme.application.query.model.ResolveTenantThemeQuery;
import com.tchalanet.server.core.tenanttheme.application.query.model.TenantThemeView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API pour gérer le thème d'un tenant (lifecycle). */
@RestController
@RequestMapping("/tenant/theme")
@RequiredArgsConstructor
@Tags({@Tag(name = "Tenant • Theme")})
public class TenantThemeController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @Operation(summary = "Get effective tenant theme")
  @GetMapping
  @PreAuthorize("hasAuthority('TENANT_READ')")
  public ResponseEntity<TenantThemeView> getTenantTheme(@CurrentContext TchRequestContext context) {
    TenantId tenantId = context.tenantId();
    var theme = queryBus.ask(new ResolveTenantThemeQuery(tenantId));
    return theme != null ? ResponseEntity.ok(theme) : ResponseEntity.noContent().build();
  }

  @Operation(summary = "Apply theme preset to tenant")
  @PostMapping
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ResponseEntity<Void> applyTheme(
      @CurrentContext TchRequestContext context, @RequestBody ApplyThemeRequest request) {
    TenantId tenantId = context.tenantId();
    commandBus.execute(new ApplyTenantThemeCommand(tenantId, request.presetCode()));
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Deactivate tenant theme (reset to default)")
  @DeleteMapping
  @PreAuthorize("hasAuthority('TENANT_ADMIN')")
  public ResponseEntity<Void> deactivateTheme(@CurrentContext TchRequestContext context) {
    TenantId tenantId = context.tenantId();
    commandBus.execute(new DeactivateTenantThemeCommand(tenantId));
    return ResponseEntity.noContent().build();
  }

  public record ApplyThemeRequest(String presetCode) {}
}
