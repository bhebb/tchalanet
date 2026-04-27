package com.tchalanet.server.features.tenantadmin.policies;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitAssignmentResult;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitDefinitionResult;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitAssignmentResult;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitDefinitionResult;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitAssignmentsView;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitDefinitionsView;
import com.tchalanet.server.features.tenantadmin.policies.model.UpsertLimitAssignmentRequest;
import com.tchalanet.server.features.tenantadmin.policies.model.UpsertLimitDefinitionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${tch.web.paths.tenant_admin:/api/v1/tenant-admin}/policies/limits")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminPoliciesLimitsController {

  private final TenantAdminPoliciesOrchestrator orchestrator;

  @GetMapping("/definitions")
  public ApiResponse<ListLimitDefinitionsView> listDefinitions() {
    return ApiResponse.success(orchestrator.listLimitDefinitions());
  }

  @PostMapping("/definitions")
  public ApiResponse<UpsertLimitDefinitionResult> upsertDefinition(@Valid @RequestBody UpsertLimitDefinitionRequest req) {
    var dto = new TenantAdminPoliciesOrchestrator.UpsertLimitDefinitionCmd(req.ruleKey(), req.enabled(), req.onBreach(), req.params(), req.appliesTo());
    return ApiResponse.success(orchestrator.upsertLimitDefinition(dto));
  }

  @DeleteMapping("/definitions/{id}")
  public ApiResponse<DeleteLimitDefinitionResult> deleteDefinition(@PathVariable com.tchalanet.server.common.types.id.LimitDefinitionId id) {
    return ApiResponse.success(orchestrator.deleteLimitDefinition(id));
  }

  @GetMapping("/assignments")
  public ApiResponse<ListLimitAssignmentsView> listAssignments(
      @RequestParam("target") com.tchalanet.server.common.types.enums.TargetType targetType,
      @RequestParam(value = "targetId", required = false) java.util.UUID targetId
  ) {
    var target = switch (targetType) {
      case TENANT -> com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget.tenant();
      case OUTLET -> com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget.outlet(com.tchalanet.server.common.types.id.OutletId.of(targetId));
      case TERMINAL -> com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget.terminal(com.tchalanet.server.common.types.id.TerminalId.of(targetId));
      case AGENT -> com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget.agent(com.tchalanet.server.common.types.id.AgentId.of(targetId));
      default -> throw new IllegalArgumentException("Unsupported targetType: " + targetType);
    };

    return ApiResponse.success(orchestrator.listAssignmentsByTarget(target));
  }

  @PostMapping("/assignments")
  public ApiResponse<UpsertLimitAssignmentResult> upsertAssignment(@Valid @RequestBody UpsertLimitAssignmentRequest req) {
    var dto = new TenantAdminPoliciesOrchestrator.UpsertLimitAssignmentCmd(req.limitDefinitionId(), req.target(), req.enabled(), req.startsAt(), req.endsAt(), req.paramsOverride(), req.appliesToOverride());
    return ApiResponse.success(orchestrator.upsertLimitAssignment(dto));
  }

  @DeleteMapping("/assignments/{id}")
  public ApiResponse<DeleteLimitAssignmentResult> deleteAssignment(@PathVariable com.tchalanet.server.common.types.id.LimitAssignmentId id) {
    return ApiResponse.success(orchestrator.deleteLimitAssignment(id));
  }
}
