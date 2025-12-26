package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.UserId;

public record SuspendUserCommand(UserId userId, String reason) implements Command<Void> {}
