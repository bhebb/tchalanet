package com.tchalanet.server.core.sales.internal.application.service.sell.generation;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.core.sales.api.model.selection.SelectionGenerationPurpose;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.core.selection.api.model.Selection;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultSelectionGenerationService implements SelectionGenerationService {

    private final RandomSelectionGenerator randomSelectionGenerator;
    private final SelectionApi selectionApi;

    public DefaultSelectionGenerationService(
        RandomSelectionGenerator randomSelectionGenerator,
        SelectionApi selectionApi
    ) {
        this.randomSelectionGenerator = randomSelectionGenerator;
        this.selectionApi = selectionApi;
    }

    @Override
    public Selection generate(
        GameCode gameCode,
        BetType betType,
        Short betOption,
        SelectionGenerationStrategy strategy,
        SelectionGenerationPurpose purpose
    ) {
        Objects.requireNonNull(gameCode, "gameCode");
        Objects.requireNonNull(betType, "betType");
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(purpose, "purpose");

        if (!gameCode.supports(betType)) {
            throw new IllegalArgumentException(
                "selection.generation.bet_type_not_supported_for_game: "
                    + gameCode + "/" + betType
            );
        }
        if (strategy != SelectionGenerationStrategy.RANDOM) {
            throw new IllegalArgumentException(
                "selection.generation.strategy_unsupported: " + strategy
            );
        }

        var raw = randomSelectionGenerator.generateRaw(betType, betOption);
        return selectionApi.canonicalize(betType, betOption, raw);
    }
}
