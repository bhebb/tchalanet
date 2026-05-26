package com.tchalanet.server.core.terminal.api.query;

public enum TerminalOperation {
  SELL_TICKET,
  SELL_PHONE,
  PAYOUT_CLAIM,
  PRINT_TICKET,
  REPRINT_TICKET,
  OFFLINE_GRANT,
  OFFLINE_SYNC,
  SCAN_TICKET,

  /**
   * Compatibility value for the current session-close flow. It should disappear
   * once close-session no longer asks terminal validation for a business action.
   */
  @Deprecated
  CANCEL
}
