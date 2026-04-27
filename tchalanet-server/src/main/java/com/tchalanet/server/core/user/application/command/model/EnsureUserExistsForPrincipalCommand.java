package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import java.time.ZoneId;
import java.util.Locale;

public record EnsureUserExistsForPrincipalCommand(
    KeycloakUserSub keycloakSub,
    String tenantCode,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName,
    String phone,
    Locale locale,
    ZoneId timeZone)
    implements Command<EnsureUserExistsForPrincipalResult> {}
