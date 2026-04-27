package com.tchalanet.server.features.platformadmin.settingsglobal;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/settings-global")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformAdminSettingsGlobalController {

  private final PlatformAdminSettingsGlobalOrchestrator orchestrator;

  @GetMapping
  public ApiResponse<SettingsGlobalOverviewView> overview(@CurrentContext TchRequestContext ctx) {
    return ApiResponse.success(orchestrator.overview());
  }
}
