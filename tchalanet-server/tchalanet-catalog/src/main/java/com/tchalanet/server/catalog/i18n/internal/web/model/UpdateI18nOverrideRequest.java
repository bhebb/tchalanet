package com.tchalanet.server.catalog.i18n.internal.web.model;

import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;
import com.tchalanet.server.catalog.i18n.api.model.I18nSurface;

/**
 * Update I18n Override Request (patch semantics — null fields = no change).
 */
public record UpdateI18nOverrideRequest(
    I18nOverrideLevel level,
    I18nSurface surface,
    String i18nValue,
    Boolean active) {}
