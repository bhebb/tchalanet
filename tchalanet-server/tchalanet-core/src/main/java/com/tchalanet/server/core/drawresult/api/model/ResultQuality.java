package com.tchalanet.server.core.drawresult.api.model;

/** Qualité du résultat de tirage. */
public enum ResultQuality {

  /**
   * Résultat complet, cohérent, conforme aux règles attendues (nombre de chiffres, format, source
   * fiable). Peut être appliqué automatiquement.
   */
  COMPLETE,

  /**
   * Résultat partiel ou incohérent : mauvais nombre de chiffres, champ manquant, parsing douteux,
   * ou source instable.
   *
   * <p>Stockable, mais nécessite revue humaine.
   */
  SUSPECT,

  /**
   * Résultat techniquement invalide : impossible à parser, données contradictoires, ou erreur
   * explicite du provider.
   *
   * <p>Ne doit pas être appliqué automatiquement.
   */
  INVALID;

  public boolean canAutoApply() {
    return this == COMPLETE;
  }
}
