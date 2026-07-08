package com.tchalanet.server.platform.tenant.api.model.view;

public record TenantInternalRules(
    TenantPromotionRules promotions,
    TenantBusinessCalendarRules businessCalendar
    // Other rule groups added here as the tenant config model evolves.
) {
    public record TenantPromotionRules(Boolean maryajGratisEnabled) {}
}
