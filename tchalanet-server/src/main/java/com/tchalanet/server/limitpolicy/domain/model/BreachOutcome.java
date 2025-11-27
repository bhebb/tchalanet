package com.tchalanet.server.limitpolicy.domain.model;

/** Defines the action to take when a limit policy is breached. */
public enum BreachOutcome {
  /** The action (e.g., ticket creation) is blocked. */
  BLOCK,
  /** The action is allowed, but a warning is logged. */
  WARN,
  /** The action is allowed without any warning. */
  ALLOW
}
