package com.tchalanet.server.core.limitpolicy.application.query.model.evaluation;

import com.tchalanet.server.common.types.enums.BreachOutcome;

import java.util.List;

public record LimitEvaluationView(
    BreachOutcome outcome,
    List<LimitBreachView> breaches
) {}
