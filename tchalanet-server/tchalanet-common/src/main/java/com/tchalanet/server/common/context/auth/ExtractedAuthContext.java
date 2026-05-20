package com.tchalanet.server.common.context.auth;

import com.tchalanet.server.common.security.TchRole;

import java.util.Set;

public record ExtractedAuthContext(
    String originalTenantCode,
    String effectiveTenantCode,
    String keycloakUserId,
    boolean overridden,
    Set<TchRole> systemRoles,
    Set<String> customRoles) {}
