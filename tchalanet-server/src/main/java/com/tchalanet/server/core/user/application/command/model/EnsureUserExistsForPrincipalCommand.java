package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

public record EnsureUserExistsForPrincipalCommand(
    UUID keycloakId,
    String tenantCode,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName,
    String phone,
    String locale,
    String timeZone)
    implements Command<EnsureUserExistsForPrincipalResult> {}
