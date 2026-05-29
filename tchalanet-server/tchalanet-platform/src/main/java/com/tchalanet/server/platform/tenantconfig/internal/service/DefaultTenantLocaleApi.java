package com.tchalanet.server.platform.tenantconfig.internal.service;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantconfig.api.TenantLocaleApi;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
class DefaultTenantLocaleApi implements TenantLocaleApi {

    // V1 stub: returns French as the default locale for Tchalanet Haiti.
    // Extend with tenant-specific config when locale preferences are stored.

    @Override
    public Locale resolveDefaultLocale(TenantId tenantId) {
        return Locale.FRENCH;
    }

    @Override
    public String resolveDefaultLanguage(TenantId tenantId) {
        return "fr";
    }
}
