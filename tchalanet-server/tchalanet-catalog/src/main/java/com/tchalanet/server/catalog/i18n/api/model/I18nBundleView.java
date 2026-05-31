package com.tchalanet.server.catalog.i18n.api.model;

import java.util.Map;

/**
 * Runtime read response: i18n translations grouped by surface.
 *
 * <p>Used by public and tenant runtime endpoints. Distinct from the admin CRUD {@link I18nOverrideView}.
 */
public record I18nBundleView(
    String locale,
    Map<I18nSurface, Map<String, String>> surfaces
) {}
