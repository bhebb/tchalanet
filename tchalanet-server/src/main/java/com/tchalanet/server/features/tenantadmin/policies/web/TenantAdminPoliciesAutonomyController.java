package com.tchalanet.server.features.tenantadmin.policies.web;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyOverviewView;
import com.tchalanet.server.features.tenantadmin.policies.TenantAdminPoliciesOrchestrator;
import com.tchalanet.server.features.tenantadmin.policies.model.UpsertAutonomyRuleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${tch.web.paths.tenant_admin:/api/v1/tenant-admin}/policies/autonomy")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminPoliciesAutonomyController {

  private final TenantAdminPoliciesOrchestrator orchestrator;

  @GetMapping("/overview")
  public ApiResponse<AutonomyOverviewView> getOverview(
      @RequestParam("targetType") com.tchalanet.server.common.types.enums.AutonomyTargetType targetType,
      @RequestParam(value = "targetId", required = false) java.util.UUID targetId) {
    return ApiResponse.success(orchestrator.getAutonomyOverview(targetType, targetId));
  }

  @PostMapping("/rules")
  public ApiResponse<AutonomyOverviewView> upsert(@CurrentContext com.tchalanet.server.common.context.TchRequestContext ctx, @Valid @RequestBody UpsertAutonomyRuleRequest req) {
    return ApiResponse.success(orchestrator.upsertAutonomyRule(ctx, req));
  }
}
