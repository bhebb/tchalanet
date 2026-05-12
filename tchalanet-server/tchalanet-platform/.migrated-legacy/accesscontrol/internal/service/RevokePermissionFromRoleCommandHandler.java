package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RevokePermissionFromRoleRequestHandler
    implements CommandHandler<RevokePermissionFromRoleRequest, Boolean> {

  private final RolePermissionAdminPort rolePermissionAdminPort;

  @Override
  public Boolean handle(RevokePermissionFromRoleRequest command) {
    return rolePermissionAdminPort.revoke(command.roleId(), command.permissionCode());
  }
}

