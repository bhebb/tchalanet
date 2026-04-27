package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.common.types.enums.BreachOutcome;

import java.util.List;

public record LimitEvaluationView(
    BreachOutcome outcome,
    List<com.tchalanet.server.core.limitpolicy.application.query.model.LimitBreachView> breaches
) {
}
