package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.annotation.Nullable;

public record ApproveUserCommand(UserId userId, @Nullable UserId approvedBy) implements Command<Void> {}
