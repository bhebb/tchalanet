package com.tchalanet.server.platform.identity.api.model.view;

import com.tchalanet.server.catalog.theme.api.ThemeMode;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.UserId;

public record UserProfileView(
    UserId id,
    KeycloakUserSub keycloakSub,
    String username,
    String email,
    String phone,
    UserStatus status,
    String firstName,
    String lastName,
    String displayName,
    ThemeMode themeMode,
    Short density,
    String locale,
    String timeZone,
    String currency) {}
