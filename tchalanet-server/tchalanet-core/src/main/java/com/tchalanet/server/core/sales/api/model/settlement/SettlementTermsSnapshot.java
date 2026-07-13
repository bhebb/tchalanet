package com.tchalanet.server.core.sales.api.model.settlement;

import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import java.util.List;
import java.util.Objects;

public record SettlementTermsSnapshot(
    int schemaVersion,
    SelectionPolicy selectionPolicy,
    List<SettlementTermSnapshot> terms
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public SettlementTermsSnapshot {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("settlement.snapshot.schema_version_required");
        }
        Objects.requireNonNull(selectionPolicy, "settlement.snapshot.selection_policy_required");
        terms = terms == null ? List.of() : List.copyOf(terms);
        if (terms.isEmpty()) {
            throw new IllegalArgumentException("settlement.snapshot.terms_required");
        }
    }

    public static SettlementTermsSnapshot current(
        SelectionPolicy selectionPolicy,
        List<SettlementTermSnapshot> terms
    ) {
        return new SettlementTermsSnapshot(CURRENT_SCHEMA_VERSION, selectionPolicy, terms);
    }
}
