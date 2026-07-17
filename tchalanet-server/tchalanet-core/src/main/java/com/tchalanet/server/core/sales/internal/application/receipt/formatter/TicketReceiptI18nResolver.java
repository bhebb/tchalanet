package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import java.util.LinkedHashMap;
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
    var values =
        tenantId == null
            ? catalog.resolveLocale(localeTag)
            : catalog.resolveLocaleForTenant(localeTag, tenantId);
    if (values.isEmpty()
        && locale != null
        && locale.getLanguage() != null
        && !locale.getLanguage().isBlank()) {
      values =
          tenantId == null
              ? catalog.resolveLocale(locale.getLanguage())
              : catalog.resolveLocaleForTenant(locale.getLanguage(), tenantId);
    }
    return new TicketReceiptTranslations(values, localeTag);
  }

  private String localeTag(Locale locale) {
    if (locale == null || locale.toLanguageTag().isBlank()) {
      return Locale.FRENCH.toLanguageTag();
    }
    return locale.toLanguageTag();
  }

  public record TicketReceiptTranslations(Map<String, String> values, String localeTag) {

    public TicketReceiptTranslations(Map<String, String> values) {
      this(values, Locale.FRENCH.toLanguageTag());
    }

    public String text(String key) {
      var value = values == null ? null : values.get(key);
      if (value != null && !value.isBlank()) {
        return value;
      }
      return fallback(localeTag, key);
    }

    private static String fallback(String localeTag, String key) {
      var language = Locale.forLanguageTag(localeTag == null ? "fr" : localeTag).getLanguage();
      var fallback = FALLBACKS.get(language);
      if (fallback == null) {
        fallback = FALLBACKS.get(Locale.FRENCH.getLanguage());
      }
      return fallback.getOrDefault(key, key);
    }
  }

  private static final Map<String, Map<String, String>> FALLBACKS = buildFallbacks();

  private static Map<String, Map<String, String>> buildFallbacks() {
    var fr = new LinkedHashMap<String, String>();
    fr.put(TicketReceiptI18nKeys.COPY_ORIGINAL, "Original");
    fr.put(TicketReceiptI18nKeys.COPY_DUPLICATE, "Copie");
    fr.put(TicketReceiptI18nKeys.COPY_REPRINT, "Réimpression");
    fr.put(TicketReceiptI18nKeys.TICKET, "Ticket");
    fr.put(TicketReceiptI18nKeys.PUBLIC_CODE, "Code public");
    fr.put(TicketReceiptI18nKeys.SALE_TIMESTAMP, "Vente");
    fr.put(TicketReceiptI18nKeys.TERMINAL, "Terminal");
    fr.put(TicketReceiptI18nKeys.SELLER, "Vendeur");
    fr.put(TicketReceiptI18nKeys.DRAW_SECTION, "Tirage");
    fr.put(TicketReceiptI18nKeys.DRAW_FULL_PREFIX, "Tirage du");
    fr.put(TicketReceiptI18nKeys.DRAW_TIME, "Heure");
    fr.put(TicketReceiptI18nKeys.LINE_HEADER_NO, "No");
    fr.put(TicketReceiptI18nKeys.LINE_HEADER_STAKE, "Mise");
    fr.put(TicketReceiptI18nKeys.TOTAL_STAKE, "Mise");
    fr.put(TicketReceiptI18nKeys.TOTAL_AMOUNT, "TOTAL");
    fr.put(TicketReceiptI18nKeys.VERIFICATION, "Vérification");
    fr.put(TicketReceiptI18nKeys.QR, "QR");
    fr.put(TicketReceiptI18nKeys.REF, "Réf.");
    fr.put(TicketReceiptI18nKeys.SCAN_TO_VERIFY, "Scannez pour vérifier");
    fr.put(TicketReceiptI18nKeys.CURRENCY_NOTE, "Montants en {code}");

    var en = new LinkedHashMap<String, String>();
    en.put(TicketReceiptI18nKeys.COPY_ORIGINAL, "Original");
    en.put(TicketReceiptI18nKeys.COPY_DUPLICATE, "Copy");
    en.put(TicketReceiptI18nKeys.COPY_REPRINT, "Reprint");
    en.put(TicketReceiptI18nKeys.TICKET, "Ticket");
    en.put(TicketReceiptI18nKeys.PUBLIC_CODE, "Public code");
    en.put(TicketReceiptI18nKeys.SALE_TIMESTAMP, "Sale");
    en.put(TicketReceiptI18nKeys.TERMINAL, "Terminal");
    en.put(TicketReceiptI18nKeys.SELLER, "Seller");
    en.put(TicketReceiptI18nKeys.DRAW_SECTION, "Draw");
    en.put(TicketReceiptI18nKeys.DRAW_FULL_PREFIX, "Draw for");
    en.put(TicketReceiptI18nKeys.DRAW_TIME, "Time");
    en.put(TicketReceiptI18nKeys.LINE_HEADER_NO, "No");
    en.put(TicketReceiptI18nKeys.LINE_HEADER_STAKE, "Stake");
    en.put(TicketReceiptI18nKeys.TOTAL_STAKE, "Stake");
    en.put(TicketReceiptI18nKeys.TOTAL_AMOUNT, "TOTAL");
    en.put(TicketReceiptI18nKeys.VERIFICATION, "Verification");
    en.put(TicketReceiptI18nKeys.QR, "QR");
    en.put(TicketReceiptI18nKeys.REF, "Ref");
    en.put(TicketReceiptI18nKeys.SCAN_TO_VERIFY, "Scan to verify");
    en.put(TicketReceiptI18nKeys.CURRENCY_NOTE, "Amounts in {code}");

    var ht = new LinkedHashMap<String, String>();
    ht.put(TicketReceiptI18nKeys.COPY_ORIGINAL, "Orijinal");
    ht.put(TicketReceiptI18nKeys.COPY_DUPLICATE, "Kopi");
    ht.put(TicketReceiptI18nKeys.COPY_REPRINT, "Reenpresyon");
    ht.put(TicketReceiptI18nKeys.TICKET, "Tikè");
    ht.put(TicketReceiptI18nKeys.PUBLIC_CODE, "Kòd piblik");
    ht.put(TicketReceiptI18nKeys.SALE_TIMESTAMP, "Vant");
    ht.put(TicketReceiptI18nKeys.TERMINAL, "Terminal");
    ht.put(TicketReceiptI18nKeys.SELLER, "Vandè");
    ht.put(TicketReceiptI18nKeys.DRAW_SECTION, "Tiraj");
    ht.put(TicketReceiptI18nKeys.DRAW_FULL_PREFIX, "Tiraj pou");
    ht.put(TicketReceiptI18nKeys.DRAW_TIME, "Lè");
    ht.put(TicketReceiptI18nKeys.LINE_HEADER_NO, "No");
    ht.put(TicketReceiptI18nKeys.LINE_HEADER_STAKE, "Miz");
    ht.put(TicketReceiptI18nKeys.TOTAL_STAKE, "Miz");
    ht.put(TicketReceiptI18nKeys.TOTAL_AMOUNT, "TOTAL");
    ht.put(TicketReceiptI18nKeys.VERIFICATION, "Verifikasyon");
    ht.put(TicketReceiptI18nKeys.QR, "QR");
    ht.put(TicketReceiptI18nKeys.REF, "Ref");
    ht.put(TicketReceiptI18nKeys.SCAN_TO_VERIFY, "Eskane pou verifye");
    ht.put(TicketReceiptI18nKeys.CURRENCY_NOTE, "Montan an {code}");

    return Map.of("fr", Map.copyOf(fr), "en", Map.copyOf(en), "ht", Map.copyOf(ht));
  }
}
