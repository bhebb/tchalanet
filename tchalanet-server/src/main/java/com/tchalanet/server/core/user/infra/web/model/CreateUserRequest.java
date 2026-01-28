package com.tchalanet.server.core.user.infra.web.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.enums.ThemeMode;
import java.util.Set;

public record CreateUserRequest(
    TenantId tenantIdInitiator,
    String email,
    String phone,
    String firstName,
    String lastName,
    ThemeMode prefThemeMode,
    Short prefDensity,
    String prefLocale,
    String prefTimeZone,
    String prefCurrency,
    boolean sendInvitation,
    Set<String> initialRoles) {}
