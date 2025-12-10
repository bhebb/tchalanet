package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.user.domain.model.AppUser;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record CreateUserCommand(
    UUID tenantIdInitiator,
    String email,
    String phone,
    String firstName,
    String lastName,
    String locale,
    boolean sendInvitation,
    Set<String> initialRoles
) implements Command<AppUser> {}
