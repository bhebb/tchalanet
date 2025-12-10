package com.tchalanet.server.core.user.infra.web.dto;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
    UUID tenantIdInitiator,
    String email,
    String phone,
    String firstName,
    String lastName,
    String locale,
    boolean sendInvitation,
    Set<String> initialRoles
) {}

