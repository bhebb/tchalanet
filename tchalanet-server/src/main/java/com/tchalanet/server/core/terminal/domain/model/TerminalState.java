package com.tchalanet.server.core.terminal.domain.model;

/** Lifecycle state of a terminal. Aligned with V100 CHECK constraint. */
public enum TerminalState {
  REGISTERED,
  ACTIVE,
  LOCKED,
  OFFLINE,
  UNREGISTERED
}
