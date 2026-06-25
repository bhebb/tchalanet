package com.tchalanet.server.features.pagemodel.dashboard;

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
 * Platform PageModel controller — resolves an allowed PageModel for super-admins.
 * The requested logicalId is still checked by PageModelAccessPolicy and by the
 * dynamic provider dispatch.
 */
@RestController
@RequestMapping("/platform/dashboard")
@RequiredArgsConstructor
@Tag(name = "Platform • PageModel")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformPageModelController {

  private final DashboardPageModelService service;

  @Operation(summary = "Resolve platform (superadmin) page model by logicalId")
  @GetMapping
  public ApiResponse<PageRuntimeResponse> platformPageModel(
      @RequestParam(name = "logicalId") String logicalId,
      @RequestParam(name = "lang", required = false) String lang) {
    return ApiResponse.success(
        service.resolve(logicalId, Optional.empty(), Optional.ofNullable(lang)));
  }
}
