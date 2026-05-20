package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.core.limitpolicy.BreachOutcome;

import java.util.List;

public record LimitEvaluationView(
    BreachOutcome outcome,
    List<LimitBreachView> breaches
) {}
