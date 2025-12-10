package com.tchalanet.server.common.domain;

public record I18nOverride(
    String tenantId,
    String locale,
    String i18nKey,
    String i18nValue
) {}
