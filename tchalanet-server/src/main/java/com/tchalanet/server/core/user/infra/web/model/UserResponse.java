package com.tchalanet.server.core.user.infra.web.model;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.KeycloakUserSub;

public record UserResponse(
    UserId id,
    KeycloakUserSub keycloakSub,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName) {}
