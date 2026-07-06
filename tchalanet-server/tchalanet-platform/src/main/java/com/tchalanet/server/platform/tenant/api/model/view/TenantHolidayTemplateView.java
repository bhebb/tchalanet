package com.tchalanet.server.platform.tenant.api.model.view;

public record TenantHolidayTemplateView(
    String key,
    String countryCode,
    String monthDay,
    Boolean defaultOpen,
    String label
) {}
