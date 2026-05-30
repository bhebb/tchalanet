package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptGameSectionView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptLabelResolver {

    public String gameTitle(
        TicketReceiptGameSectionView section,
        TicketReceiptTranslations translations
    ) {
        if (section.gameLabel() != null && !section.gameLabel().isBlank()) {
            return section.gameLabel();
        }

        var code = section.gameCode();
        var translated = translationOrNull(translations, "receipt.game." + code);

        return translated == null ? code : translated;
    }

    public String lineOptionLabel(
        TicketReceiptLineView line,
        TicketReceiptTranslations translations
    ) {
        if (line.optionLabel() != null && !line.optionLabel().isBlank()) {
            return line.optionLabel();
        }

        var betType = line.betType();
        if (betType == null || betType.isBlank() || BetType.valueOf(betType).isBorlette()) {
            return " ";
        }
        var translated = translationOrNull(translations, "receipt.bet_type." + betType);

        return " - "  + (translated == null ?  betType : translated) +" ";
    }

    private String translationOrNull(TicketReceiptTranslations translations, String key) {
        if (translations == null || key == null || key.isBlank()) {
            return null;
        }

        var value = translations.text(key);

        if (value == null || value.isBlank() || value.equals(key)) {
            return null;
        }

        return value;
    }
}

