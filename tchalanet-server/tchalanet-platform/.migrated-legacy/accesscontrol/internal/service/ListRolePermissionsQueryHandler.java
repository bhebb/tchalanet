package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.bus.QueryHandler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListRolePermissionsRequestHandler
    implements QueryHandler<ListRolePermissionsRequest, List<RolePermissionView>> {

  private final RolePermissionAdminPort rolePermissionAdminPort;

  @Override
  public List<RolePermissionView> handle(ListRolePermissionsRequest query) {
    return rolePermissionAdminPort.listPermissionCodes(query.roleId()).stream()
        .map(code -> new RolePermissionView(query.roleId(), code))
        .toList();
  }
}

