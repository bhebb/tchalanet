package com.tchalanet.server.core.user.application.command.model;
import com.tchalanet.server.common.types.id.UserId;

import com.tchalanet.server.common.bus.Command;
import java.util.Optional;
import java.util.UUID;

public record UpdateUserProfileCommand(
    UserId userId,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> email,
    Optional<String> locale
) implements Command<Void> {}
