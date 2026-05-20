package com.tchalanet.server.core.terminal.internal.domain.model;

/** Offline-sync state for a terminal. Aligned with V100 CHECK constraint. */
public enum TerminalSyncState {
  ONLINE,
  OFFLINE,
  SYNC_PENDING,
  SYNC_CONFLICT
}
