package com.tchalanet.server.features.pagemodel.dashboard;

import com.tchalanet.server.common.security.TchRole;
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
 * Platform PageModel controller — résolution du PageModel pour les super-admins.
 * [harden-pagemodel-security-v2 / D2] Le serveur résout le logicalId depuis le rôle SUPER_ADMIN.
 * Aucun logicalId arbitraire accepté.
 */
@RestController
@RequestMapping("/platform/dashboard")
@RequiredArgsConstructor
@Tag(name = "Platform • PageModel")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class PlatformPageModelController {

  private final DashboardPageModelService service;
  private final PageModelTypeResolver typeResolver;

  @Operation(summary = "Resolve platform (superadmin) page model by role (server-side resolution)")
  @GetMapping
  public ApiResponse<PageRuntimeResponse> platformPageModel(
      @RequestParam(name = "lang", required = false) String lang) {
    var type = typeResolver.forDashboard(TchRole.SUPER_ADMIN);
    return ApiResponse.success(
        service.resolve(type.logicalId(), Optional.empty(), Optional.ofNullable(lang)));
  }
}
