package com.tchalanet.server.platform.tenant.api.model.view;

public record TenantInternalRules(
    TenantBusinessCalendarRules businessCalendar
    // Other rule groups added here as the tenant config model evolves.
) {}

