package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptGameSectionView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import org.springframework.stereotype.Component;

@Component
public class TicketReceiptLabelResolver {

  public String gameTitle(
      TicketReceiptGameSectionView section, TicketReceiptTranslations translations) {
    var code = section.gameCode();
    var translated = translationOrNull(translations, "receipt.game." + code);

    if (translated != null) {
      return translated;
    }

    try {
      return switch (GameCode.valueOf(code)) {
        case HT_BOLET -> "BORLETTE";
        case HT_MARYAJ, HT_MARYAJ_GRATIS -> "MARYAJ";
        case HT_LOTO3 -> "LOTO 3 CHIFFRES";
        case HT_LOTO4 -> "LOTO 4 CHIFFRES";
        case HT_LOTO5 -> "LOTO 5 CHIFFRES";
      };
    } catch (IllegalArgumentException ex) {
      return code;
    }
  }

  public String lineOptionLabel(
      TicketReceiptLineView line, TicketReceiptTranslations translations) {
    if (line.selectionPolicySnapshot() == SelectionPolicy.EXPLICIT_ONLY
        && line.optionLabel() != null
        && !line.optionLabel().isBlank()) {
      return line.optionLabel();
    }
    return " ";
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
