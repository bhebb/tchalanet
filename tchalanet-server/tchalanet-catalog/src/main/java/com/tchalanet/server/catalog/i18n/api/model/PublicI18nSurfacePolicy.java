package com.tchalanet.server.catalog.i18n.api.model;

import java.util.Set;

public final class PublicI18nSurfacePolicy {

    private static final Set<I18nSurface> PUBLIC_SURFACES = Set.of(
        I18nSurface.PUBLIC_HOME,
        I18nSurface.PUBLIC_RESULTS,
        I18nSurface.PUBLIC_TICKET_CHECK,
        I18nSurface.COMMON_PUBLIC_ERROR
    );

    private PublicI18nSurfacePolicy() {}

    public static boolean isPublic(I18nSurface surface) {
        return PUBLIC_SURFACES.contains(surface);
    }

    public static Set<I18nSurface> publicSurfaces() {
        return PUBLIC_SURFACES;
    }
}
