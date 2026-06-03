package com.tchalanet.server.features.cashier.operationalcontext.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.List;

/**
 * Available outlets and terminals for the current seller.
 *
 * <p>Mobile UX rules:
 * <ul>
 *   <li>1 outlet + 1 terminal → auto-select without showing a picker screen.</li>
 *   <li>1 outlet + N terminals → show terminal picker only.</li>
 *   <li>N outlets → show outlet picker first, then terminal picker for the selected outlet.</li>
 * </ul>
 *
 * <p>{@code defaults} carries pre-selected ids when the list has a single choice.
 */
public record CashierOpContextOptionsView(
    List<OutletOption> outlets,
    List<TerminalOption> terminals,
    DefaultSelection defaults
) {

  public record OutletOption(
      OutletId outletId,
      String name,
      String kind
  ) {}

  public record TerminalOption(
      TerminalId terminalId,
      OutletId outletId,
      String label,
      String kind,
      // false when terminal is locked, sales-blocked, or offline-blocked
      boolean canSell
  ) {}

  /** Pre-selected ids when only one choice is available. Both may be null. */
  public record DefaultSelection(OutletId outletId, TerminalId terminalId) {}
}
