package com.tchalanet.server.features.pagemodel.dashboard.app;

import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelType;
import org.springframework.stereotype.Service;

@Service
public class PageModelTypeResolver {

    public PageModelType forPublicHome() {
        return PageModelType.PUBLIC_HOME;
    }

    public PageModelType forDashboard(TchRole role) {
        return switch (role) {
            case SUPER_ADMIN -> PageModelType.DASHBOARD_SUPERADMIN;
            case TENANT_ADMIN, SYSTEM -> PageModelType.DASHBOARD_TENANT_ADMIN;
            case OPERATOR -> PageModelType.DASHBOARD_OPERATOR;
            case CASHIER -> PageModelType.DASHBOARD_CASHIER;
        };
    }
}

