package com.tchalanet.server.features.tenantadmin.policies;

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

@RestController
@RequestMapping("/admin/policies")
@PreAuthorize("hasPermission(null, 'limit.read')")
@RequiredArgsConstructor
@Tag(name = "Tenant Admin • Policies")
public class TenantAdminPoliciesOverviewController {

  private final TenantAdminPoliciesOverviewService service;

  @GetMapping("/overview")
  @Operation(summary = "Tenant policies overview — limits summary")
  public ApiResponse<TenantAdminPoliciesOverviewView> overview(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(service.getOverview(ctx));
  }
}
