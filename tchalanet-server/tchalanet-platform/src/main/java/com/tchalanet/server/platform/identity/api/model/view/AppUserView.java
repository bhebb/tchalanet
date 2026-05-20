package com.tchalanet.server.platform.identity.api.model.view;

import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.UserId;

public record AppUserView(
    UserId id,
    KeycloakUserSub keycloakSub,
    String username,
    String email,
    String phone,
    String firstName,
    String lastName,
    String displayName,
    UserStatus status) {}
