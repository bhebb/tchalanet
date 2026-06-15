package com.tchalanet.server.features.bootstrap.publicruntime.model;

/**
 * Public (unauthenticated) runtime bootstrap.
 * Contains only what the app needs before login: branding, locale, i18n, and the first route.
 * Must NOT expose user, entitlements, notifications, or internal state.
 */
public record PublicBootstrapResponse(
    PublicSettingsView settings,
    PublicThemeView theme,
    PublicI18nBundle i18n,
    PageModelRef pageModelRef
) {}
