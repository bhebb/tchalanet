package com.tchalanet.server.core.limitpolicy.internal.domain.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;

import java.util.Comparator;
import java.util.List;

public record LimitEvaluationResult(
    BreachOutcome outcome,
    List<LimitBreach> breaches
) {

    public static LimitEvaluationResult from(List<LimitBreach> breaches) {
        var safeBreaches = breaches == null ? List.<LimitBreach>of() : List.copyOf(breaches);

        var outcome = safeBreaches.stream()
            .map(LimitBreach::outcome)
            .max(Comparator.comparingInt(LimitEvaluationResult::rank))
            .orElse(BreachOutcome.ALLOW);

        return new LimitEvaluationResult(outcome, safeBreaches);
    }

    private static int rank(BreachOutcome outcome) {
        return switch (outcome) {
            case ALLOW -> 0;
            case WARN -> 1;
            case REQUIRE_APPROVAL -> 2;
            case BLOCK -> 3;
        };
    }

    public boolean allowed() {
        return outcome == BreachOutcome.ALLOW || outcome == BreachOutcome.WARN;
    }

    public boolean blocked() {
        return outcome == BreachOutcome.BLOCK || outcome == BreachOutcome.REQUIRE_APPROVAL;
    }
}
