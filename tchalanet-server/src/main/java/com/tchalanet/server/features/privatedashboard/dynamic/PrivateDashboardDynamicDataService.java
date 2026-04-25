package com.tchalanet.server.features.privatedashboard.dynamic;

import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.privatedashboard.block.PrivateDashboardDynamicPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrivateDashboardDynamicDataService {

  private final SuperadminDashboardService superadminDashboardService;
  private final TenantAdminDashboardService tenantAdminDashboardService;
  private final CashierDashboardService cashierDashboardService;

  public PrivateDashboardDynamicPayload buildDynamicData(
      TenantId tenantId, UserId userId, TchRole role, String currentLang, PageModelDoc model) {
    return switch (role) {
      case SUPER_ADMIN -> superadminDashboardService.build(tenantId, userId, currentLang, model);
      case TENANT_ADMIN -> tenantAdminDashboardService.build(tenantId, userId, currentLang, model);
      case CASHIER -> cashierDashboardService.build(tenantId, userId, currentLang, model);
      case OPERATOR -> PrivateDashboardDynamicPayload.empty();
    };
  }
}
