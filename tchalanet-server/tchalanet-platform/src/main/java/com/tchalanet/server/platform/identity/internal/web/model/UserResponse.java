package com.tchalanet.server.platform.identity.internal.web.model;

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
