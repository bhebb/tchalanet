package com.tchalanet.server.features.pagemodel.dashboard;

import com.tchalanet.server.common.context.TchContextResolver;

import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant PageModel controller — résolution du PageModel pour les utilisateurs tenant.
 * [Phase 4B] extrait de DashboardPageModelController (routing_and_path.md — un fichier par scope)
 * [Phase 2C] @PreAuthorize isAuthenticated() sur tous les endpoints tenant
 */
@RestController
@RequestMapping("/tenant/pagemodel")
@RequiredArgsConstructor
@Tag(name = "Tenant • PageModel")
@PreAuthorize("isAuthenticated()")
public class TenantPageModelController {

  private final DashboardPageModelService service;
  private final PageModelTypeResolver typeResolver;
  private final TchContextResolver contextResolver;

  @Operation(summary = "Resolve tenant dashboard page model by role")
  @GetMapping("/dashboard")
  public ApiResponse<DashboardPageModelResponse> tenantDashboard(
      @RequestParam(name = "lang", required = false) String lang) {
    var ctxHolder = contextResolver.currentOrNull();
    var role = ctxHolder != null ? ctxHolder.currentRole() : null;
    var type = typeResolver.forDashboard(role);
    return ApiResponse.success(
        service.resolve(type.logicalId(), Optional.empty(), Optional.ofNullable(lang)));
  }

  @Operation(summary = "Resolve tenant pagemodel by logicalId")
  @GetMapping("/{logicalId}")
  public ApiResponse<DashboardPageModelResponse> tenantByLogicalId(
      @PathVariable("logicalId") String logicalId,
      @RequestParam(name = "lang", required = false) String lang) {
    return ApiResponse.success(
        service.resolve(logicalId, Optional.empty(), Optional.ofNullable(lang)));
  }
}

