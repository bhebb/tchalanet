package com.tchalanet.server.core.draw.infra.guard;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.draw.application.port.out.DrawSalesGuardPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implémentation temporaire NoOp du DrawSalesGuardPort.
 * Toutes les validations renvoient true (pas de blocage) pour permettre le développement.
 *
 * ⚠️ À REMPLACER par une vraie implémentation avant la production.
 *
 * Règles à implémenter :
 *
 * 1. assertCanCancel(drawId, force):
 *    - Bloquer si payout déjà payé
 *    - Bloquer si settlement/recompute en cours
 *    - Si tickets vendus :
 *      * sans force : bloquer
 *      * avec force : autoriser seulement si tickets peuvent être void/refund
 *    - SETTLED : bloquer sauf flow manuel spécial futur
 *
 * 2. assertCanArchive(drawId, force):
 *    - Autoriser seulement draw terminal :
 *      * CANCELED
 *      * SETTLED
 *      * éventuellement RESULTED ancien sans pending
 *    - Bloquer si tickets/payout/settlement ouverts
 *
 * 3. assertCanCorrectAppliedResult(drawId, correctedDrawResultId, force):
 *    - Bloquer si payout payé
 *    - Bloquer si recompute déjà en cours
 *    - force ne doit pas bypasser payout payé
 *    - reason obligatoire côté command/request
 */
@Component
@Slf4j
public class NoOpDrawSalesGuardAdapter implements DrawSalesGuardPort {

  @Override
  public void assertCanCancel(DrawId drawId, boolean force) {
    log.warn("NoOp DrawSalesGuardPort.assertCanCancel - drawId={}, force={} - NO VALIDATION PERFORMED",
        drawId, force);

    // TODO: Implémenter les règles :
    // - Bloquer si payout déjà payé
    // - Bloquer si settlement/recompute en cours
    // - Si tickets vendus :
    //   * sans force : bloquer
    //   * avec force : autoriser seulement si tickets peuvent être void/refund
    // - SETTLED : bloquer sauf flow manuel spécial futur
  }

  @Override
  public void assertCanArchive(DrawId drawId, boolean force) {
    log.warn("NoOp DrawSalesGuardPort.assertCanArchive - drawId={}, force={} - NO VALIDATION PERFORMED",
        drawId, force);

    // TODO: Implémenter les règles :
    // - Autoriser seulement draw terminal :
    //   * CANCELED
    //   * SETTLED
    //   * éventuellement RESULTED ancien sans pending
    // - Bloquer si tickets/payout/settlement ouverts
  }

  @Override
  public void assertCanCorrectAppliedResult(DrawId drawId, DrawResultId correctedDrawResultId, boolean force) {
    log.warn("NoOp DrawSalesGuardPort.assertCanCorrectAppliedResult - drawId={}, correctedDrawResultId={}, force={} - NO VALIDATION PERFORMED",
        drawId, correctedDrawResultId, force);

    // TODO: Implémenter les règles :
    // - Bloquer si payout payé
    // - Bloquer si recompute déjà en cours
    // - force ne doit pas bypasser payout payé
    // - reason obligatoire côté command/request
  }
}

