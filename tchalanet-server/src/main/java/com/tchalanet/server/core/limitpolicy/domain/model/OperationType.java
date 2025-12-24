package com.tchalanet.server.core.limitpolicy.domain.model;

/**
 * Enumeration of transaction operation types that can be subject to limit evaluation.
 *
 * - SALE: Ticket purchase transaction
 * - PAYOUT: Winning payout transaction
 * - CANCEL: Ticket cancellation/void transaction
 */
public enum OperationType {
  SALE,
  PAYOUT,
  CANCEL
}
