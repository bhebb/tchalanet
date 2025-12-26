package com.tchalanet.server.features.private_dashboard.dynamic;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.private_dashboard.block.PrivateDashboardDynamicPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

@Service
@RequiredArgsConstructor
public class PrivateDashboardDynamicDataService {

    private final SuperadminDashboardService superadminDashboardService;
    private final TenantAdminDashboardService tenantAdminDashboardService;
    private final CashierDashboardService cashierDashboardService;

    public PrivateDashboardDynamicPayload buildDynamicData(
        TenantId tenantId,
        UserId userId,
        TchRole role,
        String currentLang,
        PageModel model
    ) {
        return switch (role) {
            case SUPER_ADMIN -> superadminDashboardService.build(tenantId, userId, currentLang, model);
            case TENANT_ADMIN -> tenantAdminDashboardService.build(tenantId, userId, currentLang, model);
            case CASHIER -> cashierDashboardService.build(tenantId, userId, currentLang, model);
            case OPERATOR -> PrivateDashboardDynamicPayload.empty();
        };
    }
}
