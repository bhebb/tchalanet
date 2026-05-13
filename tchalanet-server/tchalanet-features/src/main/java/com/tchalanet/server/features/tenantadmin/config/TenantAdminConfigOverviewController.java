package com.tchalanet.server.features.tenantadmin.config;

import com.tchalanet.server.common.context.web.CurrentContext;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.tenantadmin.config.model.AdminConfigOverviewView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminConfigOverviewController {

  private final TenantAdminConfigOverviewOrchestrator orchestrator;

  @GetMapping("/overview")
  public ApiResponse<AdminConfigOverviewView> overview(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(orchestrator.getOverview(ctx));
  }
}
