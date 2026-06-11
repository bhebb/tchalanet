package com.tchalanet.server.core.sales.api.model.selection;

/**
 * Why a selection is being auto-generated.
 * <p>
 * PROMOTION_FREE_LINE:
 * Selection for a promotional free game line (e.g. Maryaj gratuit).
 * <p>
 * CASHIER_SUGGESTION:
 * Seller-facing "generate numbers" action on the POS, when enabled per game.
 */
public enum SelectionGenerationPurpose {
    PROMOTION_FREE_LINE,
    CASHIER_SUGGESTION
}
