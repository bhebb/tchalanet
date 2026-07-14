package com.tchalanet.server.core.sales.api.settlement;

import com.tchalanet.server.catalog.game.api.model.BetType;

/**
 * Explains the computed settlement variant of a sale line for admin/support/debug consumers.
 *
 * <p>The variant is recomputed on demand from {@code betType + betOption + selection}; it is not
 * persisted because realized payout is calculated at settlement time from sale snapshots. See
 * change {@code supported-bet-options-combinations-v1}.
 */
public interface SettlementExplanationApi {

  /**
   * Resolves the computed settlement variant and its labels.
   *
   * @param betType the line bet type (required)
   * @param betOption the line bet option code, or {@code null} for option-less bet types
   * @param selection the canonical selection played
   * @return the computed variant view
   * @throws IllegalArgumentException if the bet type/option/selection combination is unsupported
   */
  ComputedSettlementVariantView explain(BetType betType, Short betOption, String selection);
}
