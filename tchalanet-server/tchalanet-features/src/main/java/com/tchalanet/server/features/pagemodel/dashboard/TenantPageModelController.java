package com.tchalanet.server.features.pagemodel.dashboard;

import com.tchalanet.server.common.context.TchContextResolver;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.pagemodel.runtime.PageRuntimeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant PageModel controller — résolution du PageModel pour les utilisateurs tenant.
 * [harden-pagemodel-security-v2 / D2] Le client demande GET /tenant/dashboard ; le serveur
 * résout le logicalId concret depuis TchRequestContext (rôle). Aucun logicalId arbitraire accepté.
 */
@RestController
@RequestMapping("/tenant/dashboard")
@RequiredArgsConstructor
@Tag(name = "Tenant • PageModel")
@PreAuthorize("isAuthenticated()")
public class TenantPageModelController {

  private final DashboardPageModelService service;
  private final PageModelTypeResolver typeResolver;
  private final TchContextResolver contextResolver;

  @Operation(summary = "Resolve tenant page model by role (server-side resolution)")
  @GetMapping
  public ApiResponse<PageRuntimeResponse> tenantPageModel(
      @RequestParam(name = "lang", required = false) String lang) {
    var ctxHolder = contextResolver.currentOrNull();
    var role = ctxHolder != null ? ctxHolder.currentRole() : null;
    var type = typeResolver.forDashboard(role);
    var api = ApiResponse.success(
        service.resolve(type.logicalId(), Optional.empty(), Optional.ofNullable(lang)));
    return api;
  }
}
