package com.tchalanet.server.core.offlinesync.internal.infra.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.offlinesync.api.query.GetOfflineRiskDashboardQuery;
import com.tchalanet.server.core.offlinesync.api.query.OfflineRiskDashboardView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/admin/offline-sync/risk")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
public class OfflineRiskAdminController {

  private final QueryBus queryBus;

  public OfflineRiskAdminController(QueryBus queryBus) {
    this.queryBus = queryBus;
  }

  @GetMapping("/dashboard")
  public ApiResponse<OfflineRiskDashboardView> dashboard(@CurrentContext TchRequestContext ctx) {
    var dashboard = queryBus.ask(new GetOfflineRiskDashboardQuery(ctx.effectiveTenantIdRequired()));
    return ApiResponse.success(dashboard);
  }
}

