package com.tchalanet.server.catalog.settings.internal.web;

import com.tchalanet.server.catalog.settings.api.model.ResolveSettingsCriteria;
import com.tchalanet.server.catalog.settings.api.model.ResolvedSettingView;
import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Tenant Settings Controller
 *
 * <p>Provides read-only access to resolved settings for tenant contexts. This endpoint allows
 * tenants to query their effective settings.
 *
 * <p>Security: TENANT_ADMIN or TENANT_USER role required.
 *
 * <p>This controller delegates to {@link SettingsCatalog} for resolution.
 */
@RestController
@RequestMapping("/tenant/settings")
@RequiredArgsConstructor
@Tag(name = "Tenant • Settings", description = "Tenant access to resolved application settings")
public class TenantSettingsController {

  private final SettingsCatalog settingsCatalog;

  @Operation(
      summary = "Resolve effective settings",
      description =
          "Get effective settings for a tenant context with optional outlet/terminal overrides")
  @GetMapping("/resolve")
  public ApiResponse<List<ResolvedSettingView>> resolve(
      @CurrentContext TchRequestContext ctx,
      @RequestParam(required = false) OutletId outletId,
      @RequestParam(required = false) TerminalId terminalId,
      @RequestParam(required = false, defaultValue = "") List<String> namespaces) {

    var criteria = new ResolveSettingsCriteria(ctx.tenantId(), outletId, terminalId, namespaces);
    List<ResolvedSettingView> resolved = settingsCatalog.resolve(criteria);
    return ApiResponse.success(resolved);
  }
}
