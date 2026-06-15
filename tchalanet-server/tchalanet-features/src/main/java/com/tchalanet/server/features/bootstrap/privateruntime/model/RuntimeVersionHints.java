package com.tchalanet.server.features.bootstrap;

import jakarta.annotation.Nullable;

/**
 * Runtime configuration version hints. When any version changes the client calls full private
 * bootstrap once (with cooldown). Only {@code bootstrapVersion} is required.
 */
public record RuntimeVersionHints(
    String bootstrapVersion,
    @Nullable String navigationVersion,
    @Nullable String entitlementsVersion,
    @Nullable String themeVersion,
    @Nullable String i18nVersion,
    @Nullable String settingsVersion
) {
    public static com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeVersionHints of(String bootstrapVersion) {
        return new com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeVersionHints(bootstrapVersion, null, null, null, null, null);
    }
}
