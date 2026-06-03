package com.tchalanet.server.platform.tenanttheme.internal.service;

import org.springframework.stereotype.Component;

/**
 * Validates theme token overrides against preset editableTokens allowlist.
 * V1: no overrides allowed — this validator exists for V2 readiness.
 */
@Component
public class TenantThemeTokenValidator {

    public void validateOverrides(java.util.Map<String, String> overrides) {
        if (overrides != null && !overrides.isEmpty()) {
            throw new IllegalArgumentException(
                "Token overrides are not supported in V1. Deferred to V2.");
        }
    }
}
