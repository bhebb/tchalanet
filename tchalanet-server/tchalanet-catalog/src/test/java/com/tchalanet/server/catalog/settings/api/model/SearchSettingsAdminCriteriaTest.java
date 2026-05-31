package com.tchalanet.server.catalog.settings.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchSettingsAdminCriteriaTest {

    @Test
    void nullExposureMeansNoExposureFilter() {
        // Admin callers (super admin, tenant admin) pass null — no WHERE exposure = clause.
        var criteria = new SearchSettingsAdminCriteria(null, null, null, null, null, null);
        assertThat(criteria.exposure()).isNull();
    }

    @Test
    void publicRuntimeExposureIsAppliedForPublicCaller() {
        var criteria = new SearchSettingsAdminCriteria(
            null, null, null, SettingExposure.PUBLIC_RUNTIME, null, null);
        assertThat(criteria.exposure()).isEqualTo(SettingExposure.PUBLIC_RUNTIME);
    }

    @Test
    void internalExposureIsValidForBackendQueries() {
        var criteria = new SearchSettingsAdminCriteria(
            null, null, null, SettingExposure.INTERNAL, null, null);
        assertThat(criteria.exposure()).isEqualTo(SettingExposure.INTERNAL);
    }
}
