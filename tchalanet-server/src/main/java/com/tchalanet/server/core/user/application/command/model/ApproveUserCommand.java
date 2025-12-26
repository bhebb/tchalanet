package com.tchalanet.server.core.user.application.command.model;
import com.tchalanet.server.common.types.id.UserId;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

public record ApproveUserCommand(
    UserId userId,
    UUID approvedBy
) implements Command<Void> {
}

