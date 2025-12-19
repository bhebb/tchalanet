package com.tchalanet.server.core.accesscontrol.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.accesscontrol.application.command.model.GrantPermissionToRoleCommand;
import com.tchalanet.server.core.accesscontrol.application.port.out.RolePermissionAdminPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GrantPermissionToRoleCommandHandler implements CommandHandler<GrantPermissionToRoleCommand, Boolean> {

  private final RolePermissionAdminPort rolePermissionAdminPort;

  @Override
  public Boolean handle(GrantPermissionToRoleCommand command) {
    return rolePermissionAdminPort.grant(command.roleId(), command.permissionCode());
  }
}

