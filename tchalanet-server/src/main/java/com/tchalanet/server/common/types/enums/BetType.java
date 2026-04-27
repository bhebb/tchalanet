package com.tchalanet.server.common.types.enums;

/**
 * Enum décrivant les types de paris.
 * Ajout de métadonnées utilitaires (non‑breaking) : largeur canonique, indication si un betOption est requis
 * et bornes pour l'option. Les noms des constantes restent inchangés pour préserver la compatibilité DB (EnumType.STRING).
 */
public enum BetType {
  MATCH_1_2D(2),
  MATCH_2_2D(2),
  MATCH_3_2D(2),
  LOTTO3_3D(3),
  MARRIAGE_2D2D(2),
  LOTTO4_PATTERN(4, true, (short) 1, (short) 3),
  LOTTO5_PATTERN(5, true, (short) 1, (short) 3);

  private final int width; // canonical digit width per atomic selection (eg. 2 for 2D, 3 for 3D, 4/5 for patterns)
  private final boolean requiresOption; // true when betOption must be provided (eg. pattern variants)
  private final short optionMin;
  private final short optionMax;

  BetType(int width) {
    this(width, false, (short) 0, (short) 0);
  }

  BetType(int width, boolean requiresOption, short optionMin, short optionMax) {
    this.width = width;
    this.requiresOption = requiresOption;
    this.optionMin = optionMin;
    this.optionMax = optionMax;
  }

  /** Largeur canonique utilisée par les canonicalizers (ex: 2 pour 2D, 3 pour 3D). */
  public int canonicalWidth() {
    return width;
  }

  /** Indique si un betOption est requis pour ce BetType (ex: LOTTO4_PATTERN). */
  public boolean requiresBetOption() {
    return requiresOption;
  }

  /** Borne minimale (incluse) acceptée pour betOption; 0 si non applicable. */
  public short betOptionMin() {
    return optionMin;
  }

  /** Borne maximale (incluse) acceptée pour betOption; 0 si non applicable. */
  public short betOptionMax() {
    return optionMax;
  }

  /** Helper lisible pour vérifier les variantes pattern. */
  public boolean isPattern() {
    return this == LOTTO4_PATTERN || this == LOTTO5_PATTERN;
  }
}
