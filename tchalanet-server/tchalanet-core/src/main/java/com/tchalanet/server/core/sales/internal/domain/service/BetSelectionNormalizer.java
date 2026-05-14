package com.tchalanet.server.core.sales.internal.domain.service;

import com.tchalanet.server.core.selection.SelectionKeyCanonicalizer;
import com.tchalanet.server.catalog.game.api.model.BetType;

public class BetSelectionNormalizer {

    public String normalize(BetType betType, String rawSelection) {
        return SelectionKeyCanonicalizer.canonicalize(betType, rawSelection);
    }
}
