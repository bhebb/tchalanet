package com.tchalanet.server.platform.tenant.api.model.view;

import java.util.List;

/**
 * Locale configuration sub-section of the tenant config JSON.
 * Mirrors {@code tenantconfig/locale_config.json}; deep-merged into
 * {@code tenant.config} during tenant creation.
 */
public record TenantInternalLocaleConfig(
    List<String> supportedLanguages,
    String fallbackLanguage
) {
    public List<String> effectiveSupportedLanguages() {
        return supportedLanguages == null ? List.of() : List.copyOf(supportedLanguages);
    }
}
