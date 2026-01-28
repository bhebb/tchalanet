package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Locale;
import java.util.Optional;

public record UpdateUserProfileCommand(
    UserId userId,
    Optional<String> firstName,
    Optional<String> lastName,
    Optional<String> email,
    Optional<Locale> locale)
    implements Command<Void> {}
