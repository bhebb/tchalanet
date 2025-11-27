package com.tchalanet.server.pos.domain.model;

/** Defines the lifecycle states of a POS Session. */
public enum PosSessionStatus {
  /** Session is active, tickets can be sold. */
  OPEN,
  /** Session was automatically closed by the system (e.g., end of day, idle timeout). */
  AUTO_CLOSED,
  /** Session was manually closed by the user. */
  CLOSED,
  /** Session has been reconciled by an administrator (optional state). */
  SETTLED
}
