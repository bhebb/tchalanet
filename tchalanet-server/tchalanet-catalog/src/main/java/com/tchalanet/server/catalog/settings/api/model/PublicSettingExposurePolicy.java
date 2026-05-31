package com.tchalanet.server.catalog.settings.api.model;

import java.util.Set;

public final class PublicSettingExposurePolicy {

    private static final Set<SettingExposure> PUBLIC_EXPOSURES = Set.of(
        SettingExposure.PUBLIC_RUNTIME
    );

    private PublicSettingExposurePolicy() {}

    public static boolean isPublic(SettingExposure exposure) {
        return PUBLIC_EXPOSURES.contains(exposure);
    }

    public static Set<SettingExposure> publicExposures() {
        return PUBLIC_EXPOSURES;
    }
}
