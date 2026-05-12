package com.tchalanet.server.platform.accesscontrol.api.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.RoleId;

public record GrantPermissionToRoleRequest(RoleId roleId, String permissionCode)
    implements Command<Boolean> {}

