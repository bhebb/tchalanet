package com.tchalanet.server.core.draw.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.web.error.ProblemRest;

/**
 * Port pour valider les opérations sur les draws par rapport à l'état des sales.
 * Bloque certaines opérations si des tickets/payouts sont dans un état incompatible.
 */
public interface DrawSalesGuardPort {

  /**
   * Vérifie qu'un draw peut être annulé.
   *
   * @param drawId l'identifiant du draw
   * @param force si true, autorise l'annulation même avec des tickets vendus (si void/refund possible)
   * @throws ProblemRest si l'annulation est bloquée
   */
  void assertCanCancel(DrawId drawId, boolean force);

  /**
   * Vérifie qu'un draw peut être archivé.
   *
   * @param drawId l'identifiant du draw
   * @param force si true, force l'archivage (avec prudence)
   * @throws ProblemRest si l'archivage est bloqué
   */
  void assertCanArchive(DrawId drawId, boolean force);

  /**
   * Vérifie qu'on peut corriger un résultat déjà appliqué.
   *
   * @param drawId l'identifiant du draw
   * @param correctedDrawResultId le nouveau résultat à appliquer
   * @param force si true, force la correction (ne bypass pas payout payé)
   * @throws ProblemRest si la correction est bloquée
   */
  void assertCanCorrectAppliedResult(DrawId drawId, DrawResultId correctedDrawResultId, boolean force);
}

