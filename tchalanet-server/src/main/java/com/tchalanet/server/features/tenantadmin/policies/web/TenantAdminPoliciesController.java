package com.tchalanet.server.features.tenantadmin.policies.web;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.tenantadmin.policies.TenantAdminPoliciesOrchestrator;
import com.tchalanet.server.features.tenantadmin.policies.model.PoliciesOverviewView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${tch.web.paths.tenant_admin:/api/v1/tenant-admin}/policies")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminPoliciesController {

  private final TenantAdminPoliciesOrchestrator orchestrator;

  @GetMapping("/overview")
  public ApiResponse<PoliciesOverviewView> overview(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(orchestrator.getPoliciesOverview(ctx));
  }
}
