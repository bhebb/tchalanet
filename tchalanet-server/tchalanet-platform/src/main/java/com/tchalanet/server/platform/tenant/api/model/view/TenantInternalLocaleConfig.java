package com.tchalanet.server.platform.tenant.api.model.view;

import java.util.List;

/**
 * Locale configuration sub-section of the tenant config JSON.
 * Mirrors {@code tenantconfig/locale_config.json}; deep-merged into
 * {@code tenant.config} during tenant creation.
 *
 * <p>{@code defaultLanguage} / {@code defaultLocale} are also stored as fast-path
 * columns on {@code tenant} for bootstrap-hot reads; this record carries the
 * evolving options (supported languages, fallback chain).
 */
public record TenantInternalLocaleConfig(
    String defaultLanguage,
    String defaultLocale,
    List<String> supportedLanguages,
    String fallbackLanguage
) {
    public List<String> effectiveSupportedLanguages() {
        return supportedLanguages == null ? List.of() : List.copyOf(supportedLanguages);
    }
}
