package com.tchalanet.server.features.tenantadmin.config.i18n.model;

public record UpsertI18nOverrideRequest(
    String locale,
    String i18nKey,
    String i18nValue,
    Boolean active
) {}
