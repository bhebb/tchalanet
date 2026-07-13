package com.tchalanet.server.features.pos.games;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import java.util.List;

public record PosGameOptionResponse(
    GameCode gameCode,
    String gameLabel,
    BetType betType,
    String betTypeLabel,
    boolean requiresOption,
    SelectionPolicy selectionPolicy,
    List<PosBetOptionResponse> options,
    String selectionHint
) {
    public PosGameOptionResponse {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
