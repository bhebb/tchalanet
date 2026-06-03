package com.tchalanet.server.platform.tenant.internal.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.TenantLocaleApi;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalLocaleConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import com.tchalanet.server.platform.tenant.internal.port.TenantConfigReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class TenantLocaleApiAdapter implements TenantLocaleApi {

    private static final String FALLBACK_LANGUAGE = "fr";
    private static final Locale FALLBACK_LOCALE = Locale.forLanguageTag("fr-HT");
    private static final List<String> FALLBACK_SUPPORTED = List.of("fr", "ht", "en");

    private final TenantConfigReader reader;

    @Override
    public Locale resolveDefaultLocale(TenantId tenantId) {
        var tag = locale(tenantId)
            .map(TenantInternalLocaleConfig::defaultLocale)
            .filter(s -> s != null && !s.isBlank())
            .orElse(null);
        return tag == null ? FALLBACK_LOCALE : Locale.forLanguageTag(tag);
    }

    @Override
    public String resolveDefaultLanguage(TenantId tenantId) {
        return locale(tenantId)
            .map(TenantInternalLocaleConfig::defaultLanguage)
            .filter(s -> s != null && !s.isBlank())
            .orElse(FALLBACK_LANGUAGE);
    }

    @Override
    public List<String> resolveSupportedLanguages(TenantId tenantId) {
        return locale(tenantId)
            .map(TenantInternalLocaleConfig::effectiveSupportedLanguages)
            .filter(l -> !l.isEmpty())
            .orElse(FALLBACK_SUPPORTED);
    }

    private java.util.Optional<TenantInternalLocaleConfig> locale(TenantId tenantId) {
        TenantInternalSettings settings = reader.getInternalSettings(tenantId);
        if (settings == null || settings.locale() == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(settings.locale());
    }
}
