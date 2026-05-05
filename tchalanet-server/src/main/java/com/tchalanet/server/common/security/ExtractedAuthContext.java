package com.tchalanet.server.common.security;

import com.tchalanet.server.common.types.enums.TchRole;
import java.util.Set;

public record ExtractedAuthContext(
    String originalTenantCode,
    String effectiveTenantCode,
    String keycloakUserId,
    boolean overridden,
    Set<TchRole> systemRoles,
    Set<String> customRoles) {}
