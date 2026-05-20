package com.tchalanet.server.core.selection.api;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionValidationResult;


public interface SelectionApi {
    Selection canonicalize(BetType betType, String rawSelection);

    SelectionValidationResult validate(BetType betType, String rawSelection);
}
