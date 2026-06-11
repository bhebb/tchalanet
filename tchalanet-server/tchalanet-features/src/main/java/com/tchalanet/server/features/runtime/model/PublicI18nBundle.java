package com.tchalanet.server.features.runtime.model;

import java.util.Map;

/** Public i18n bundle delivered inside public bootstrap. */
public record PublicI18nBundle(
    String lang,
    Map<String, String> messages,
    String loadedAt
) {}
