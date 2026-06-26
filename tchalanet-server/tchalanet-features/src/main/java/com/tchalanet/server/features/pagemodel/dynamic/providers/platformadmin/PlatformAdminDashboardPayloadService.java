package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformAdminDashboardPayloadService {

  static final String COMMERCIAL_LOGICAL_ID = "private.dashboard.superadmin";
  static final String OPS_LOGICAL_ID = "private.dashboard.superadmin.ops";

  private final PlatformAdminDashboardPayloadAssembler commercialAssembler;
  private final PlatformAdminOpsDashboardPayloadAssembler opsAssembler;

  public Object assemble(String logicalId, TchRequestContext ctx) {
    if (logicalId == null || logicalId.isBlank()) {
      throw new PageModelDynamicProviderException(
          "PLATFORM_ADMIN_DASHBOARD_LOGICAL_ID_REQUIRED",
          "logicalId is required for source=" + PlatformAdminDashboardProvider.SOURCE);
    }

    return switch (logicalId) {
      case COMMERCIAL_LOGICAL_ID -> commercialAssembler.assemble(ctx);
      case OPS_LOGICAL_ID -> opsAssembler.assemble(ctx);
      default -> throw new PageModelDynamicProviderException(
          "PLATFORM_ADMIN_DASHBOARD_UNSUPPORTED_LOGICAL_ID",
          "Unsupported logicalId for source="
              + PlatformAdminDashboardProvider.SOURCE
              + ": "
              + logicalId);
    };
  }
}
