package com.tchalanet.server.common.types.enums;

/** Qualité du résultat de tirage. */
public enum ResultQuality {

  /**
   * Résultat complet, cohérent, conforme aux règles attendues (nombre de chiffres, format, source
   * fiable). → Peut être appliqué automatiquement.
   */
  COMPLETE,

  /**
   * Résultat partiel ou incohérent : - mauvais nombre de chiffres - champ manquant - parsing
   * douteux - source instable
   *
   * <p>→ Stockable, mais nécessite revue humaine.
   */
  SUSPECT,

  /**
   * Résultat techniquement invalide : - impossible à parser - données contradictoires - erreur
   * explicite du provider
   *
   * <p>→ Ne doit PAS être appliqué automatiquement.
   */
  INVALID,

  PARTIAL
}
