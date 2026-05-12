package com.tchalanet.server.common.types.enums;

/**
 * Scope d'idempotency = "quel type de commande" on déduplique.
 * Stable (API contract) : ne jamais renommer sans migration.
 */
public enum IdempotencyScope {
  SALES_SELL_TICKET,
  SALES_APPROVE_TICKET,
  SALES_REJECT_TICKET,
  SALES_CANCEL_TICKET,
  SALES_OVERRIDE_TICKET_RESULT,

  // plus tard…
  PAYOUT_CREATE,
  DRAW_OPEN,
  DRAW_CLOSE
}
