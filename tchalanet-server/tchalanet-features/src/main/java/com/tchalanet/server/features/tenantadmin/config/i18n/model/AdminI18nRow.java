package com.tchalanet.server.features.tenantadmin.config.i18n.model;

public record AdminI18nRow(
    String id,
    String locale,
    String i18nKey,
    String i18nValue,
    String level,
    Boolean active
) {}
