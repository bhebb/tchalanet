package com.tchalanet.server.features.tenantadmin.overview;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant overview endpoint — structural diagnosis and navigation.
 *
 * Spec dashboard-overview-runtime-v1 §tenant-admin-runtime:
 *   - feature endpoint, not a PageModel provider
 *   - MUST NOT repeat dashboard KPIs
 *   - returns sections with status, summary, issues and route
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Tenant Admin • Overview")
public class TenantAdminOverviewController {

  private final TenantAdminOverviewService service;

  @GetMapping("/overview")
  @Operation(summary = "Tenant overview — structural sections, status and routes")
  public ApiResponse<TenantAdminOverviewView> overview(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(service.getOverview(ctx));
  }
}
