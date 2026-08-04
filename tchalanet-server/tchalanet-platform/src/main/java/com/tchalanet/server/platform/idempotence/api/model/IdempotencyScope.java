package com.tchalanet.server.platform.idempotence.api.model;

/**
 * Scope d'idempotency = "quel type de commande" on déduplique. Stable (API contract) : ne jamais
 * renommer sans migration.
 */
public enum IdempotencyScope {
  SALES_SELL_TICKET,
  SALES_CANCEL_TICKET,
  SALES_OVERRIDE_TICKET_RESULT,
  ANALYTICS_RECONCILIATION,

  // plus tard...
  PAYOUT_CREATE,
  DRAW_OPEN,
  DRAW_CLOSE
}
