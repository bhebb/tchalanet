package com.tchalanet.server.platform.tenant.internal.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
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

    private final TenantPreContextLookupApi registry;
    private final TenantConfigReader reader;

    @Override
    public Locale resolveDefaultLocale(TenantId tenantId) {
        var tag = registry.findById(tenantId)
            .map(view -> view.defaultLocale())
            .filter(value -> value != null && !value.isBlank())
            .orElse(null);
        return tag == null ? FALLBACK_LOCALE : Locale.forLanguageTag(tag);
    }

    @Override
    public String resolveDefaultLanguage(TenantId tenantId) {
        return registry.findById(tenantId)
            .map(view -> view.defaultLanguage())
            .filter(value -> value != null && !value.isBlank())
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
