package com.tchalanet.server.core.accesscontrol.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

public record UpdateRoleCommand(
    UUID id,
    String code,
    String name,
    String description,
    UUID tenantId,
    UUID parentRoleId,
    boolean system
) implements Command<UUID> {}

