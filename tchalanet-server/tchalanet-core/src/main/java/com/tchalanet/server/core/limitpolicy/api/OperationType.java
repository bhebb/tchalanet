package com.tchalanet.server.core.limitpolicy.api;

/**
 * Enumeration of transaction operation types that can be subject to limit evaluation.
 *
 * <p>- SALE: Ticket purchase transaction - PAYOUT: Winning payout transaction - CANCEL: Ticket
 * cancellation/void transaction
 */
public enum OperationType {
  SALE,
  PAYOUT,
  CANCEL
}
