package com.tchalanet.server.features.pagemodel.dashboard;

import com.tchalanet.server.common.security.TchRole;
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
 * Platform PageModel controller — résolution du PageModel pour les super-admins.
 * [Phase 4B] extrait de DashboardPageModelController (routing_and_path.md — un fichier par scope)
 * [Phase 2C] @PreAuthorize SUPER_ADMIN sur tous les endpoints platform
 */
@RestController
@RequestMapping("/platform/pagemodel")
@RequiredArgsConstructor
@Tag(name = "Platform • PageModel")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class PlatformPageModelController {

  private final DashboardPageModelService service;
  private final PageModelTypeResolver typeResolver;

  @Operation(summary = "Resolve platform (superadmin) dashboard page model")
  @GetMapping("/dashboard")
  public ApiResponse<DashboardPageModelResponse> platformDashboard(
      @RequestParam(name = "lang", required = false) String lang) {
    var type = typeResolver.forDashboard(TchRole.SUPER_ADMIN);
    return ApiResponse.success(
        service.resolve(type.logicalId(), Optional.empty(), Optional.ofNullable(lang)));
  }

  @Operation(summary = "Resolve platform pagemodel by logicalId")
  @GetMapping("/{logicalId}")
  public ApiResponse<DashboardPageModelResponse> platformByLogicalId(
      @PathVariable("logicalId") String logicalId,
      @RequestParam(name = "lang", required = false) String lang) {
    return ApiResponse.success(
        service.resolve(logicalId, Optional.empty(), Optional.ofNullable(lang)));
  }
}

