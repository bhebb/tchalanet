package com.tchalanet.server.core.user.application.command.model;

import java.util.UUID;
import com.tchalanet.server.common.bus.Command;

public record DeleteUserCommand(UUID userId) implements Command<Void> {}




