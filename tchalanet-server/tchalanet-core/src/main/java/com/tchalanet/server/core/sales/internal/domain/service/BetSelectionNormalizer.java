package com.tchalanet.server.core.sales.internal.domain.service;

import com.tchalanet.server.common.selection.SelectionKeyCanonicalizer;
import com.tchalanet.server.common.types.enums.BetType;

public class BetSelectionNormalizer {

    public String normalize(BetType betType, String rawSelection) {
        return SelectionKeyCanonicalizer.canonicalize(betType, rawSelection);
    }
}
