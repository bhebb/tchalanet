package com.tchalanet.server.core.limitpolicy.api;

public enum RuleKey {

  // Bloque une ou plusieurs sélections pour un tirage donné.
  BLOCK_SELECTION_PER_DRAW,

  // Mise totale maximum déjà exposée sur une sélection pour un tirage donné (cumulatif).
  MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW,
}
