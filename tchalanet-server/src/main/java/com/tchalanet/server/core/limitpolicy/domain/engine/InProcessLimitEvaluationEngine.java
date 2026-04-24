package com.tchalanet.server.core.limitpolicy.domain.engine;

import com.tchalanet.server.core.limitpolicy.application.query.model.LimitEvaluationView;
import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimits;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;

/** Minimal engine interface and default stub implementation */
public interface InProcessLimitEvaluationEngine {
  LimitEvaluationView evaluate(EffectiveLimits resolvedDefs, LimitFactsSnapshot facts, LimitContext ctx);
}

