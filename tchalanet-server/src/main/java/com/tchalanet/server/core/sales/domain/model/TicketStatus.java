package com.tchalanet.server.core.sales.domain.model;

/** Defines the lifecycle states of a Ticket. */
public enum TicketStatus {
  /** The ticket has been sold. */
  SOLD,
  /** The ticket has been administratively voided and is no longer valid. */
  VOIDED,
  /** The ticket has winning lines after draw result. */
  RESULTED_WIN,
  /** The ticket has no winning lines after draw result. */
  RESULTED_LOSS,
  /** The ticket's payout is pending approval/payment. */
  PAYMENT_PENDING,
  /** The winnings for this ticket have been paid out. */
  PAID
}
