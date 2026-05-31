package com.tchalanet.server.catalog.i18n.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchI18nOverridesCriteriaTest {

    @Test
    void emptyCriteriaHasNullSurfaces() {
        var criteria = SearchI18nOverridesCriteria.empty();
        assertThat(criteria.surfaces()).isNull();
    }

    @Test
    void nullSurfacesMeansNoSurfaceFilter() {
        // Admin callers (super admin, tenant admin) pass null surfaces — no WHERE surface IN clause.
        var criteria = new SearchI18nOverridesCriteria(null, null, null, null, null, null, null);
        assertThat(criteria.surfaces()).isNull();
    }

    @Test
    void publicSurfaceSetIsAppliedForPublicCaller() {
        var publicSurfaces = PublicI18nSurfacePolicy.publicSurfaces();
        var criteria = new SearchI18nOverridesCriteria(
            null, null, null, null, null, publicSurfaces, null);
        assertThat(criteria.surfaces()).isEqualTo(publicSurfaces);
        assertThat(criteria.surfaces()).doesNotContain(I18nSurface.CASHIER, I18nSurface.INTERNAL);
    }
}
