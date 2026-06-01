package com.tchalanet.server.platform.identity.internal.model;

import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.UserId;

public record UserRow(
    UserId id,
    KeycloakUserSub keycloakSub,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName,
    String status) {}
