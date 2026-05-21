package com.tchalanet.server.features.cashier.games;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import java.util.List;

public record CashierGameOptionResponse(
    GameCode gameCode,
    String gameLabel,
    BetType betType,
    String betTypeLabel,
    boolean requiresOption,
    List<CashierBetOptionResponse> options,
    String selectionHint
) {
    public CashierGameOptionResponse {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
