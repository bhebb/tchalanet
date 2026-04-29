package com.tchalanet.server.core.limitpolicy.application.service;

import com.tchalanet.server.core.limitpolicy.application.port.out.ExposureFactsReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.LimitEvaluationView;
import com.tchalanet.server.core.limitpolicy.domain.engine.InProcessLimitEvaluationEngine;
import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimits;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;
import com.tchalanet.server.core.limitpolicy.domain.resolver.LimitResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LimitPolicyRuntimeService {

  private final LimitDefinitionReaderPort definitions;

  private final LimitAssignmentReaderPort assignments;

  @Autowired(required = false)
  private ExposureFactsReaderPort exposureFacts;

  @Autowired(required = false)
  private InProcessLimitEvaluationEngine engine;

  private final LimitResolver resolver;

  public LimitEvaluationView evaluate(LimitContext ctx) {
    var defs = definitions.listActive();
    var assigns = assignments.listActiveForTargets(ctx.toTargets(), Instant.now());

    EffectiveLimits resolved = resolver.resolve(defs, assigns, ctx);
    LimitFactsSnapshot facts = (exposureFacts != null) ? exposureFacts.snapshot(ctx) : LimitFactsSnapshot.EMPTY;

    if (engine != null) return engine.evaluate(resolved, facts, ctx);

    // permissive default
    return new LimitEvaluationView(com.tchalanet.server.common.types.enums.BreachOutcome.ALLOW, java.util.List.of());
  }
}
