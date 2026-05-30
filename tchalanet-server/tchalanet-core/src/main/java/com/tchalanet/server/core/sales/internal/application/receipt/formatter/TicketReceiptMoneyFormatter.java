package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptMoneyFormatter {

    public String format(Money money, TicketReceiptLayoutProfile profile) {
        if (money == null) {
            return null;
        }

        var amount = money.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (profile != null && profile.compactCurrencyDisplay()) {
            return amount;
        }
        // Default display: currency before amount (e.g. "HTG 10.00") for receipts/messages
        return money.currency().code() + " " + amount;
    }

    /**
     * For receipt paper (compact currency display) provide a single-line currency note
     * e.g. "Montants en HTG" to avoid repeating the currency on every line.
     */
    public String currencyNote(Money money, TicketReceiptLayoutProfile profile, TicketReceiptTranslations translations) {
        if (money == null || profile == null) {
            return null;
        }
        if (!profile.compactCurrencyDisplay()) {
            return null;
        }
        var code = money.currency() == null ? null : money.currency().code();
        if (code == null || code.isBlank()) {
            return null;
        }

        if (translations != null) {
            var translated = translations.text(TicketReceiptI18nKeys.CURRENCY_NOTE);
            if (translated != null && !translated.isBlank()) {
                // allow placeholder like "Montants en {code}" substitution
                if (translated.contains("{code}")) {
                    return translated.replace("{code}", code);
                }
                return translated;
            }
        }

        return "Montants en " + code;
    }
}
