package com.tchalanet.server.features.private_dashboard.dynamic;

import com.tchalanet.server.core.accesscontrol.domain.model.TchRole;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.private_dashboard.block.PrivateDashboardDynamicPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivateDashboardDynamicDataService {

    private final SuperadminDashboardService superadminDashboardService;
    private final TenantAdminDashboardService tenantAdminDashboardService;
    private final CashierDashboardService cashierDashboardService;

    public PrivateDashboardDynamicPayload buildDynamicData(
        UUID tenantId,
        UUID userId,
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
