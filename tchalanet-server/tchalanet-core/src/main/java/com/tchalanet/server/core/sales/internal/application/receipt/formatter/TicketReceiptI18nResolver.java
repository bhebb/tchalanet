package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Locale;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketReceiptI18nResolver {

    private final I18nOverridesCatalog catalog;

    public TicketReceiptTranslations resolve(Locale locale, TenantId tenantId) {
        var localeTag = localeTag(locale);
        var values = tenantId == null
            ? catalog.resolveLocale(localeTag)
            : catalog.resolveLocaleForTenant(localeTag, tenantId);
        if (values.isEmpty() && locale != null && locale.getLanguage() != null && !locale.getLanguage().isBlank()) {
            values = tenantId == null
                ? catalog.resolveLocale(locale.getLanguage())
                : catalog.resolveLocaleForTenant(locale.getLanguage(), tenantId);
        }
        return new TicketReceiptTranslations(values);
    }

    private String localeTag(Locale locale) {
        if (locale == null || locale.toLanguageTag().isBlank()) {
            return Locale.FRENCH.toLanguageTag();
        }
        return locale.toLanguageTag();
    }

    public record TicketReceiptTranslations(Map<String, String> values) {

        public String text(String key) {
            var value = values == null ? null : values.get(key);
            return value == null || value.isBlank() ? key : value;
        }
    }
}
