package com.tchalanet.server.catalog.i18n.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublicI18nSurfacePolicyTest {

    @Test
    void publicSurfacesContainsExactlyFourValues() {
        assertThat(PublicI18nSurfacePolicy.publicSurfaces()).containsExactlyInAnyOrder(
            I18nSurface.PUBLIC_HOME,
            I18nSurface.PUBLIC_RESULTS,
            I18nSurface.PUBLIC_TICKET_CHECK,
            I18nSurface.COMMON_PUBLIC_ERROR
        );
    }

    @Test
    void isPublicReturnsTrueForAllPublicSurfaces() {
        for (var surface : PublicI18nSurfacePolicy.publicSurfaces()) {
            assertThat(PublicI18nSurfacePolicy.isPublic(surface))
                .as("expected isPublic=true for %s", surface)
                .isTrue();
        }
    }

    @Test
    void isPublicReturnsFalseForPrivateSurfaces() {
        for (var surface : new I18nSurface[]{
            I18nSurface.AUTH,
            I18nSurface.CASHIER,
            I18nSurface.TENANT_ADMIN,
            I18nSurface.PLATFORM_ADMIN,
            I18nSurface.COMMON_PRIVATE_ERROR,
            I18nSurface.INTERNAL
        }) {
            assertThat(PublicI18nSurfacePolicy.isPublic(surface))
                .as("expected isPublic=false for %s", surface)
                .isFalse();
        }
    }

    @Test
    void publicSurfacesDoesNotContainPrivateSurfaces() {
        var pub = PublicI18nSurfacePolicy.publicSurfaces();
        assertThat(pub).doesNotContain(
            I18nSurface.AUTH,
            I18nSurface.CASHIER,
            I18nSurface.TENANT_ADMIN,
            I18nSurface.PLATFORM_ADMIN,
            I18nSurface.COMMON_PRIVATE_ERROR,
            I18nSurface.INTERNAL
        );
    }

    @Test
    void publicSurfacesIsUnmodifiable() {
        var surfaces = PublicI18nSurfacePolicy.publicSurfaces();
        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> ((java.util.Set<I18nSurface>) surfaces).add(I18nSurface.CASHIER));
    }
}
