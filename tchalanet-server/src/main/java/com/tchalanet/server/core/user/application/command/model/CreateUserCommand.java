package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.ThemeMode;

import java.util.Optional;
import java.util.Set;

public record CreateUserCommand(
    String email,
    String phone,
    String firstName,
    String lastName,
    Optional<ThemeMode> prefThemeMode,
    Optional<Short> prefDensity,
    Optional<String> prefLocale,
    Optional<String> prefTimeZone,
    Optional<String> prefCurrency,
    boolean sendInvitation,
    Set<String> initialRoles)
    implements Command<CreateUserResult> {
}
