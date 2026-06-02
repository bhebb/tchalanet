package com.tchalanet.server.platform.tenanttheme.api.model;

import java.util.Map;

/**
 * Safe public/private runtime theme view.
 * Never exposes: tenantId, raw preset config, audit fields, arbitrary metadata.
 */
public record ThemeRuntimeView(
    String presetCode,
    String mode,
    Map<String, String> tokens,
    boolean isDefault,
    long version
) {}
