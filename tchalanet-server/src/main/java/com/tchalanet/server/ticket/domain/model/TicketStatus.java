package com.tchalanet.server.ticket.domain.model;

/** Defines the lifecycle states of a Ticket. */
public enum TicketStatus {
  /** The ticket is active and awaiting draw results. */
  PENDING,
  /** The ticket contains one or more winning lines. */
  WON,
  /** The ticket has no winning lines. */
  LOST,
  /** The winnings for this ticket have been paid out. */
  PAID,
  /** The ticket has been administratively voided and is no longer valid. */
  VOID
}
