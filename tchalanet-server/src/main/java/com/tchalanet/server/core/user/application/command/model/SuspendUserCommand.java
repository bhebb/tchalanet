package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

public record SuspendUserCommand(
    UUID userId,
    String reason
) implements Command<Void> {
}

