package com.tchalanet.server.core.sales.internal.application.service.sell.generation;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.core.sales.api.model.selection.SelectionGenerationPurpose;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import com.tchalanet.server.core.selection.api.model.Selection;

/**
 * Generates a valid, canonical selection for a given game, according to an
 * authorised strategy, in a sale or promotion context.
 * <p>
 * Promotion never generates numbers: it decides the effect, sales generates
 * and materialises (see {@code DOMAIN_SALES.md} §11 and
 * {@code promotion_design.md} §16).
 */
public interface SelectionGenerationService {

    /**
     * @param gameCode  game the selection is generated for; must support {@code betType}
     * @param betType   bet type driving the selection shape (width, pair, pattern)
     * @param betOption required when {@code betType} has options (e.g. Maryaj exact/revers)
     * @param strategy  V1: {@link SelectionGenerationStrategy#RANDOM} only
     * @param purpose   audit/extension context of the generation
     * @return a canonical {@link Selection} valid for the game rules
     * @throws IllegalArgumentException when the strategy is unsupported or the
     *                                  game/betType/betOption combination is invalid
     */
    Selection generate(
        GameCode gameCode,
        BetType betType,
        Short betOption,
        SelectionGenerationStrategy strategy,
        SelectionGenerationPurpose purpose
    );
}
