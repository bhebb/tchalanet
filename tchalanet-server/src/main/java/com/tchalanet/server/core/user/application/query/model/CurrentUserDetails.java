package com.tchalanet.server.core.user.application.query.model;

import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.UserId;

public record CurrentUserDetails(
    UserId id,
    KeycloakUserSub keycloakSub,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName,
    TenantContext tenant,
    UserPreferenceDetails preferences,
    EffectiveUiContext effective) {
}
