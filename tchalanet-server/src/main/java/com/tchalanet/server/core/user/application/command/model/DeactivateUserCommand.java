package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.user.domain.model.AppUser;

import java.util.Optional;
import java.util.UUID;

public record DeactivateUserCommand(
    UUID userId,
    UUID performedBy,
    Optional<String> reason) implements Command<AppUser> {}
