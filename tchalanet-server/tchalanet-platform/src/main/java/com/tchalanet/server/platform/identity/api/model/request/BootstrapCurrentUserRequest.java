package com.tchalanet.server.platform.identity.api.model.request;

import com.tchalanet.server.common.types.id.KeycloakUserSub;
import java.time.ZoneId;
import java.util.Locale;

public record BootstrapCurrentUserRequest(
    KeycloakUserSub keycloakSub,
    String tenantCode,
    String username,
    String email,
    String phone,
    String firstName,
    String lastName,
    String displayName,
    Locale locale,
    ZoneId timeZone) {}
