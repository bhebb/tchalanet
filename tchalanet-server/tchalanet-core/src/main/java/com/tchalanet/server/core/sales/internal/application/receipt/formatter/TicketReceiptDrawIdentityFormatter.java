package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptDrawIdentityFormatter {

    public String label(TicketReceiptView receipt, TicketReceiptTranslations translations) {
        if (receipt == null) {
            return null;
        }
        var fullLabel = firstTranslated(
            translations,
            key("receipt.draw.identity.", receipt.resultSlotKey()),
            key("receipt.draw.identity.", receipt.drawChannelCode())
        );
        if (fullLabel != null) {
            return fullLabel;
        }

        var providerCode = providerCode(receipt.resultProvider(), receipt.resultSlotKey(), receipt.drawChannelCode());
        var slotCode = slotCode(receipt.resultSlotKey());
        var structured = join(
            firstTranslated(translations, key("receipt.draw.provider.", providerCode)),
            firstTranslated(
                translations,
                key("receipt.draw.slot.", receipt.resultSlotKey()),
                key("receipt.draw.slot.", slotCode)
            )
        );
        var coded = join(providerCode, slotCode);
        return firstNonBlank(structured, coded, receipt.drawChannelLabel(), receipt.drawLabel(), receipt.drawChannelCode());
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
        return parts.length == 0 ? normalized.toUpperCase(Locale.ROOT) : parts[parts.length - 1].toUpperCase(Locale.ROOT);
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

}
