package com.tchalanet.server.catalog.drawchannel.api.resolver;

import java.util.Locale;

/**
 * Résout les clés i18n pour les draw channels publics.
 *
 * <p>Construit la clé i18n à partir du slotKey selon le format :
 * {@code draw_channel.{provider}.{period}.label}
 *
 * <p>Exemples :
 * <ul>
 *   <li>NY_MID → draw_channel.ny.mid.label</li>
 *   <li>FL_EVE → draw_channel.fl.eve.label</li>
 *   <li>TX_1800 → draw_channel.tx.1800.label</li>
 * </ul>
 *
 * <p>Cette classe évite d'avoir à stocker les label_key dans la BD et permet une
 * construction dynamique cohérente côté backend.
 */
public final class DrawChannelLabelKeyResolver {

  private DrawChannelLabelKeyResolver() {
    // Utility class
  }

  /**
   * Résout la clé i18n à partir du slot key.
   *
   * @param slotKey Clé technique du slot (ex: "NY_MID", "FL_EVE", "TX_1800")
   * @return Clé i18n (ex: "draw_channel.ny.mid.label") ou null si slotKey invalide
   */
  public static String resolve(String slotKey) {
    if (slotKey == null || slotKey.isBlank()) {
      return null;
    }

    var normalized = slotKey.trim().toUpperCase(Locale.ROOT);
    var parts = normalized.split("_", 2);

    if (parts.length < 2) {
      // Format invalide - devrait avoir au moins PROVIDER_PERIOD
      return null;
    }

    var provider = parts[0].toLowerCase(Locale.ROOT);
    var period = parts[1].toLowerCase(Locale.ROOT);

    return "draw_channel." + provider + "." + period + ".label";
  }
}

