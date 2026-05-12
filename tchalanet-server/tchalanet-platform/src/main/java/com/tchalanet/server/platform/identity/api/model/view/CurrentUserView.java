package com.tchalanet.server.platform.identity.api.model.view;

import com.tchalanet.server.common.types.enums.ThemeMode;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record CurrentUserView(
    UserId id,
    KeycloakUserSub keycloakSub,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName,
    TenantId tenantId,
    String tenantCode,
    String tenantTimeZone,
    String tenantCurrency,
    ThemeMode themeMode,
    Short density,
    String locale,
    String timeZone,
    String currency) {}
