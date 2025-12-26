package com.tchalanet.server.core.user.application.command.model;
import com.tchalanet.server.common.types.id.UserId;

import java.util.UUID;
import com.tchalanet.server.common.bus.Command;

public record DeleteUserCommand(UserId userId) implements Command<Void> {}




