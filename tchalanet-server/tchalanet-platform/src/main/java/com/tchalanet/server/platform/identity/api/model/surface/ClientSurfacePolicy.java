package com.tchalanet.server.platform.identity.api.model.surface;

import com.tchalanet.server.common.security.TchRole;
import java.util.EnumSet;
import java.util.Set;

public final class ClientSurfacePolicy {

  private ClientSurfacePolicy() {}

  public static ClientSurface preferredSurface(Set<TchRole> roles) {
    if (hasRole(roles, TchRole.SUPER_ADMIN)) {
      return ClientSurface.PLATFORM_ADMIN_WEB;
    }
    if (hasRole(roles, TchRole.TENANT_ADMIN) || hasRole(roles, TchRole.SYSTEM)) {
      return ClientSurface.TENANT_ADMIN_WEB;
    }
    if (hasRole(roles, TchRole.CASHIER) || hasRole(roles, TchRole.OPERATOR)) {
      return ClientSurface.MOBILE_POS;
    }
    return ClientSurface.CASHIER_WEB;
  }

  public static Set<ClientSurface> availableSurfaces(Set<TchRole> roles) {
    EnumSet<ClientSurface> surfaces = EnumSet.noneOf(ClientSurface.class);
    if (hasRole(roles, TchRole.CASHIER) || hasRole(roles, TchRole.OPERATOR)) {
      surfaces.add(ClientSurface.MOBILE_POS);
      surfaces.add(ClientSurface.CASHIER_WEB);
    }
    if (hasRole(roles, TchRole.TENANT_ADMIN) || hasRole(roles, TchRole.SYSTEM)) {
      surfaces.add(ClientSurface.TENANT_ADMIN_WEB);
      surfaces.add(ClientSurface.CASHIER_WEB);
    }
    if (hasRole(roles, TchRole.SUPER_ADMIN)) {
      surfaces.add(ClientSurface.PLATFORM_ADMIN_WEB);
    }
    if (surfaces.isEmpty()) {
      surfaces.add(ClientSurface.CASHIER_WEB);
    }
    return Set.copyOf(surfaces);
  }

  private static boolean hasRole(Set<TchRole> roles, TchRole role) {
    return roles != null && roles.contains(role);
  }
}
