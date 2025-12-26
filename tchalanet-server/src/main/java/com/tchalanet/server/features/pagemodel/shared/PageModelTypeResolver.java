package com.tchalanet.server.features.pagemodel.shared;

import com.tchalanet.server.common.security.TchRole;
import org.springframework.stereotype.Service;

@Service
public class PageModelTypeResolver {

  public PageModelType forPublicHome() {
    return PageModelType.PUBLIC_HOME;
  }

  public PageModelType forDashboard(TchRole role) {
    return switch (role) {
      case SUPER_ADMIN -> PageModelType.DASHBOARD_SUPERADMIN;
      case TENANT_ADMIN -> PageModelType.DASHBOARD_TENANT_ADMIN;
      case OPERATOR -> PageModelType.DASHBOARD_OPERATOR;
      case CASHIER -> PageModelType.DASHBOARD_CASHIER;
    };
  }
}
