package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.user.infra.web.dto.MeResponse;
import java.util.Set;
import java.util.UUID;

public record EnsureUserExistsForPrincipalCommand(
    String keycloakId,
    String email,
    String firstName,
    String lastName,
    Set<UUID> tenantIdsFromToken) implements Command<MeResponse> {}
