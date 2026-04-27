package com.tchalanet.server.core.accesscontrol.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.RoleId;

public record GrantPermissionToRoleCommand(RoleId roleId, String permissionCode)
    implements Command<Boolean> {}
