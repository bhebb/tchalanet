package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.user.domain.model.AppUser;
import java.util.UUID;

public record ReplaceUserCommand(
    UUID userId,
    String username,
    String email,
    String phone,
    String firstName,
    String lastName,
    String displayName,
    String avatarUrl,
    String status,
    String locale,
    String timeZone
) implements Command<AppUser> {}

