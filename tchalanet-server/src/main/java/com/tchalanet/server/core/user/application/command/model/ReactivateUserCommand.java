package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.UserId;

public record ReactivateUserCommand(UserId userId) implements Command<Void> {}
