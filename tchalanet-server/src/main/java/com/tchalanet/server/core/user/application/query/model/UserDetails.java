package com.tchalanet.server.core.user.application.query.model;

import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.enums.ThemeMode;
import java.time.Instant;
import java.util.Set;
import java.util.Locale;
import java.time.ZoneId;
import java.util.Currency;

public record UserDetails(
    UserId id,
    KeycloakUserSub keycloakSub,
    TenantId tenantId,
    OutletId outletId,
    String username,
    String name,
    String firstName,
    String lastName,
    String phone,
    String email,
    String status,
    String displayName,
    Set<String> roles,
    Locale locale,
    ZoneId timeZone,
    Instant lastLoginAt,
    ThemeMode themeMode,
    Short density,
    Locale preferenceLocale,
    Currency preferenceCurrency) {}
