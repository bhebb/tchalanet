package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GrantPermissionToRoleRequestHandler
    implements CommandHandler<GrantPermissionToRoleRequest, Boolean> {

  private final RolePermissionAdminPort rolePermissionAdminPort;

  @Override
  public Boolean handle(GrantPermissionToRoleRequest command) {
    return rolePermissionAdminPort.grant(command.roleId(), command.permissionCode());
  }
}

