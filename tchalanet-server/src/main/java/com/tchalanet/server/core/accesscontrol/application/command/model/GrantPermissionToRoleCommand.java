package com.tchalanet.server.core.accesscontrol.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

public record GrantPermissionToRoleCommand(UUID roleId, String permissionCode)
    implements Command<Boolean> {}
