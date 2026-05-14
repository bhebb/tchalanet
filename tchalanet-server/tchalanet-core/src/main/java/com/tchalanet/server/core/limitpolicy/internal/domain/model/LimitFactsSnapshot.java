package com.tchalanet.server.core.limitpolicy.internal.domain.model;

import com.tchalanet.server.catalog.game.api.model.BetType;

import java.util.Map;

public record LimitFactsSnapshot(
    Map<Key, Fact> facts
) {

    public LimitFactsSnapshot {
        facts = facts == null ? Map.of() : Map.copyOf(facts);
    }

    public Fact fact(
        LimitScopeRef scope,
        BetType betType,
        String selectionKey
    ) {
        return facts.getOrDefault(
            new Key(scope, betType, selectionKey),
            Fact.ZERO);
    }

    public record Key(
        LimitScopeRef scope,
        BetType betType,
        String selectionKey
    ) {}

    public record Fact(
        long stakeTotalCents,
        long potentialPayoutTotalCents,
        long salesCount
    ) {
        public static final Fact ZERO = new Fact(0L, 0L, 0L);
    }
}
