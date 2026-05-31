package com.tchalanet.server.catalog.settings.internal.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.settings.api.model.SettingExposure;

import org.junit.jupiter.api.Test;

class SettingsRegistryExposureTest {

    @Test
    void knownPublicRuntimeKeysAreMarkedPublicRuntime() {
        assertThat(SettingsRegistry.UI_DEFAULT_LOCALE.defaultExposure())
            .isEqualTo(SettingExposure.PUBLIC_RUNTIME);
        assertThat(SettingsRegistry.UI_SUPPORTED_LOCALES.defaultExposure())
            .isEqualTo(SettingExposure.PUBLIC_RUNTIME);
        assertThat(SettingsRegistry.UI_PUBLIC_HOME_VARIANT.defaultExposure())
            .isEqualTo(SettingExposure.PUBLIC_RUNTIME);
    }

    @Test
    void allOtherRegistryKeysDefaultToInternal() {
        var publicKeys = java.util.Set.of(
            SettingsRegistry.UI_DEFAULT_LOCALE.fullKey(),
            SettingsRegistry.UI_SUPPORTED_LOCALES.fullKey(),
            SettingsRegistry.UI_PUBLIC_HOME_VARIANT.fullKey());

        for (var def : SettingsRegistry.all()) {
            if (!publicKeys.contains(def.fullKey())) {
                assertThat(def.defaultExposure())
                    .as("expected INTERNAL for %s", def.fullKey())
                    .isEqualTo(SettingExposure.INTERNAL);
            }
        }
    }

    @Test
    void defaultConstructorSetsInternalExposure() {
        var def = new SettingKeyDef<>("test", "key",
            com.tchalanet.server.catalog.settings.api.model.SettingValueType.STRING, "v");
        assertThat(def.defaultExposure()).isEqualTo(SettingExposure.INTERNAL);
        assertThat(def.exposureOverridable()).isTrue();
    }
}
