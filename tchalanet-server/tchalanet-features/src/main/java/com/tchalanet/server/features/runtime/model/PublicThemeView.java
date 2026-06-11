package com.tchalanet.server.features.runtime.model;

import jakarta.annotation.Nullable;

/**
 * Global Tchalanet public theme. Tenant theme is not used on public pages unless tenant-branded
 * public pages are introduced later.
 */
public record PublicThemeView(
    String scope,
    String mode,
    String primaryColor,
    String secondaryColor,
    @Nullable String logoUrl,
    @Nullable String faviconUrl
) {
    public static PublicThemeView fallback() {
        return new PublicThemeView("PUBLIC", "light", "#020135", "#fecb01", null, null);
    }
}
