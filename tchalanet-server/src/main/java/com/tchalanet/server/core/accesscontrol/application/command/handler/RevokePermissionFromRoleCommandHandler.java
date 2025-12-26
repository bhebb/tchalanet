package com.tchalanet.server.core.accesscontrol.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.accesscontrol.application.command.model.RevokePermissionFromRoleCommand;
import com.tchalanet.server.core.accesscontrol.application.port.out.RolePermissionAdminPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RevokePermissionFromRoleCommandHandler
    implements CommandHandler<RevokePermissionFromRoleCommand, Boolean> {

  private final RolePermissionAdminPort rolePermissionAdminPort;

  @Override
  public Boolean handle(RevokePermissionFromRoleCommand command) {
    return rolePermissionAdminPort.revoke(command.roleId(), command.permissionCode());
  }
}
