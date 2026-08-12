package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptDrawIdentityFormatter {

  public String label(TicketReceiptView receipt, TicketReceiptTranslations translations) {
    if (receipt == null) {
      return null;
    }
    var providerCode =
        providerCode(receipt.resultProvider(), receipt.resultSlotKey(), receipt.drawChannelCode());
    var slotCode = slotCode(receipt.resultSlotKey());
    var structured =
        join(
            officialProviderName(providerCode),
            firstTranslated(
                translations,
                key("receipt.draw.slot.", receipt.resultSlotKey()),
                key("receipt.draw.slot.", slotCode)));
    var coded = join(providerCode, slotCode);
    return firstNonBlank(
        structured,
        coded,
        receipt.drawChannelLabel(),
        receipt.drawLabel(),
        receipt.drawChannelCode());
  }

  public String label(TicketReceiptView receipt) {
    return label(receipt, null);
  }

  private String providerCode(String explicit, String resultSlotKey, String channelCode) {
    var normalized = clean(explicit);
    if (normalized != null) {
      return normalized.split("[_-]", 2)[0].toUpperCase(Locale.ROOT);
    }
    normalized = clean(resultSlotKey);
    if (normalized != null) {
      return normalized.split("[_-]", 2)[0].toUpperCase(Locale.ROOT);
    }
    normalized = clean(channelCode);
    if (normalized != null) {
      var parts = normalized.split("[_-]");
      return parts.length > 1 && "HT".equalsIgnoreCase(parts[0])
          ? parts[1].toUpperCase(Locale.ROOT)
          : parts[0].toUpperCase(Locale.ROOT);
    }
    return null;
  }

  private String slotCode(String resultSlotKey) {
    var normalized = clean(resultSlotKey);
    if (normalized == null) {
      return null;
    }
    var parts = normalized.split("[_-]");
    return parts.length == 0
        ? normalized.toUpperCase(Locale.ROOT)
        : parts[parts.length - 1].toUpperCase(Locale.ROOT);
  }

  private String officialProviderName(String providerCode) {
    var code = clean(providerCode);
    if (code == null) {
      return null;
    }
    return OFFICIAL_PROVIDER_NAMES.getOrDefault(code.toUpperCase(Locale.ROOT), code);
  }

  private String join(String left, String right) {
    if (left == null || left.isBlank()) {
      return right;
    }
    if (right == null || right.isBlank() || right.equals(left)) {
      return left;
    }
    return left + " · " + right;
  }

  private String firstNonBlank(String... values) {
    for (var value : values) {
      var cleaned = clean(value);
      if (cleaned != null) {
        return cleaned;
      }
    }
    return null;
  }

  private String firstTranslated(TicketReceiptTranslations translations, String... keys) {
    if (translations == null) {
      return null;
    }
    for (var key : keys) {
      var cleanedKey = clean(key);
      if (cleanedKey == null) {
        continue;
      }
      var translated = translations.text(cleanedKey);
      if (translated != null && !translated.isBlank() && !translated.equals(cleanedKey)) {
        return translated;
      }
    }
    return null;
  }

  private String clean(String value) {
    if (value == null) {
      return null;
    }
    var trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String key(String prefix, String value) {
    var cleaned = clean(value);
    return cleaned == null ? null : prefix + cleaned.toUpperCase(Locale.ROOT);
  }

  private static final Map<String, String> OFFICIAL_PROVIDER_NAMES =
      Map.ofEntries(
          Map.entry("CA", "California"),
          Map.entry("FL", "Florida"),
          Map.entry("GA", "Georgia"),
          Map.entry("IL", "Illinois"),
          Map.entry("MI", "Michigan"),
          Map.entry("MN", "Minnesota"),
          Map.entry("MO", "Missouri"),
          Map.entry("MS", "Mississippi"),
          Map.entry("NJ", "New Jersey"),
          Map.entry("NY", "New York"),
          Map.entry("OH", "Ohio"),
          Map.entry("PA", "Pennsylvania"),
          Map.entry("TN", "Tennessee"),
          Map.entry("TX", "Texas"),
          Map.entry("WI", "Wisconsin"));
}
