package com.tchalanet.server.core.selection.internal.application;


import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.core.selection.api.model.SelectionValidationError;
import com.tchalanet.server.core.selection.api.model.SelectionValidationResult;
import com.tchalanet.server.core.selection.internal.domain.service.SelectionKeyCanonicalizer;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultSelectionApi implements SelectionApi {

    @Override
    public Selection canonicalize(BetType betType, String rawSelection) {
        var key = SelectionKeyCanonicalizer.canonicalize(betType, rawSelection);
        return new Selection(key, key.value());
    }

    @Override
    public SelectionValidationResult validate(BetType betType, String rawSelection) {
        try {
            return SelectionValidationResult.valid(canonicalize(betType, rawSelection));
        } catch (IllegalArgumentException e) {
            return SelectionValidationResult.invalid(List.of(
                new SelectionValidationError("selection.invalid", e.getMessage())
            ));
        }
    }
}
