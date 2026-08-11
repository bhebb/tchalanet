package com.tchalanet.server.core.sales.internal.application.receipt.formatter;

import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptGameSectionView;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptLineView;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver.TicketReceiptTranslations;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import java.util.Locale;
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
      var gameCode = GameCode.valueOf(code);
      if (isHaitianCreole(translations)) {
        return switch (gameCode) {
          case HT_BOLET -> "Bòlèt";
          case HT_MARYAJ -> "Maryaj";
          case HT_MARYAJ_GRATIS -> "Maryaj gratis";
          case HT_LOTO3 -> "Loto 3 chif";
          case HT_LOTO4 -> "Loto 4 chif";
          case HT_LOTO5 -> "Loto 5 chif";
        };
      }
      return switch (gameCode) {
        case HT_BOLET -> "Borlette";
        case HT_MARYAJ -> "Maryaj";
        case HT_MARYAJ_GRATIS -> "Maryaj gratis";
        case HT_LOTO3 -> "Loto 3 chiffres";
        case HT_LOTO4 -> "Loto 4 chiffres";
        case HT_LOTO5 -> "Loto 5 chiffres";
      };
    } catch (IllegalArgumentException ex) {
      return code;
    }
  }

  private boolean isHaitianCreole(TicketReceiptTranslations translations) {
    if (translations == null || translations.localeTag() == null) {
      return false;
    }
    return "ht".equals(Locale.forLanguageTag(translations.localeTag()).getLanguage());
  }

  public String lineOptionLabel(
      TicketReceiptLineView line, TicketReceiptTranslations translations) {
    if (line.selectionPolicySnapshot() == SelectionPolicy.EXPLICIT_ONLY
        && line.optionLabel() != null
        && !line.optionLabel().isBlank()) {
      return compactOptionLabel(line.optionLabel());
    }
    return " ";
  }

  private String compactOptionLabel(String label) {
    var normalized = label.trim();
    return switch (normalized) {
      case "Ordre exact" -> "Exact";
      case "Revers / Double" -> "Revers";
      case "Exact + Permuté", "Exact + Box" -> "Exact+Box";
      case "Permuté", "Désordre / Box" -> "Box";
      case "Avant 2 chiffres" -> "Avant";
      case "Arrière 2 chiffres" -> "Arrière";
      case "1er et 2e lots" -> "Lot 1-2";
      case "1er et 3e lots" -> "Lot 1-3";
      case "Mixte 1er/2e/3e lot" -> "Mixte 1-2-3";
      default -> normalized;
    };
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
