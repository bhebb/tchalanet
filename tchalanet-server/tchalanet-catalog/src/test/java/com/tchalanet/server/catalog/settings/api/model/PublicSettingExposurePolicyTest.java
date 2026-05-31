package com.tchalanet.server.catalog.settings.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublicSettingExposurePolicyTest {

    @Test
    void publicExposuresContainsOnlyPublicRuntime() {
        assertThat(PublicSettingExposurePolicy.publicExposures())
            .containsExactly(SettingExposure.PUBLIC_RUNTIME);
    }

    @Test
    void isPublicReturnsTrueOnlyForPublicRuntime() {
        assertThat(PublicSettingExposurePolicy.isPublic(SettingExposure.PUBLIC_RUNTIME)).isTrue();
    }

    @Test
    void isPublicReturnsFalseForAllOtherExposures() {
        for (var exposure : new SettingExposure[]{
            SettingExposure.INTERNAL,
            SettingExposure.TENANT_RUNTIME,
            SettingExposure.ADMIN_RUNTIME
        }) {
            assertThat(PublicSettingExposurePolicy.isPublic(exposure))
                .as("expected isPublic=false for %s", exposure)
                .isFalse();
        }
    }

    @Test
    void publicExposuresIsUnmodifiable() {
        var exposures = PublicSettingExposurePolicy.publicExposures();
        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> ((java.util.Set<SettingExposure>) exposures).add(SettingExposure.INTERNAL));
    }
}
