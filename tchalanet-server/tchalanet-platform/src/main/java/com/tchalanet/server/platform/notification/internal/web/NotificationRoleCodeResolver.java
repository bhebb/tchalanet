package com.tchalanet.server.platform.notification.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.security.TchRole;

final class NotificationRoleCodeResolver {

  private NotificationRoleCodeResolver() {}

  static String resolve(TchRequestContext context) {
    if (context.tenantId() != null) {
      if (context.roleCodes().contains(TchRole.TENANT_OWNER.name())
          || context.systemRoles().contains(TchRole.TENANT_OWNER)) {
        return TchRole.TENANT_OWNER.name();
      }
      if (context.roleCodes().contains(TchRole.TENANT_ADMIN.name())
          || context.systemRoles().contains(TchRole.TENANT_ADMIN)) {
        return TchRole.TENANT_ADMIN.name();
      }
      return null;
    }

    if (context.roleCodes().contains(TchRole.SUPER_ADMIN.name())
        || context.systemRoles().contains(TchRole.SUPER_ADMIN)) {
      return TchRole.SUPER_ADMIN.name();
    }
    return context.currentRole() == null ? null : context.currentRole().name();
  }
}
