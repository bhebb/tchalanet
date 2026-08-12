package com.tchalanet.server.core.limitpolicy.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.api.query.EffectiveLimitsForDrawView;
import com.tchalanet.server.core.limitpolicy.api.query.GetEffectiveLimitsForDrawQuery;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.internal.domain.resolver.LimitResolver;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetEffectiveLimitsForDrawQueryHandler
    implements QueryHandler<GetEffectiveLimitsForDrawQuery, EffectiveLimitsForDrawView> {

  private final LimitAssignmentReaderPort assignments;
  private final LimitResolver resolver;
  private final Clock clock;

  @Override
  public EffectiveLimitsForDrawView handle(GetEffectiveLimitsForDrawQuery q) {
    var now = clock.instant();

    var ctx =
        new LimitContext(
            q.tenantId(),
            null,
            null, // no sellerTerminalId — admin overview is not contextualized to a terminal
            null,
            q.drawChannelId(),
            now,
            List.of());

    var effectiveLimits = resolver.resolve(assignments.listActiveForTargets(ctx.scopes(), now), ctx);

    var rules =
        effectiveLimits.rules().entrySet().stream()
            .map(
                entry ->
                    new EffectiveLimitsForDrawView.ResolvedRule(
                        entry.getKey().name(),
                        scopeName(entry.getValue().appliedScope()),
                        true))
            .toList();

    return new EffectiveLimitsForDrawView(rules);
  }

  private String scopeName(com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef scope) {
    return switch (scope) {
      case com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef.TenantScope ignored ->
          "TENANT";
      case com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef.DrawChannelScope
          ignored -> "DRAW_CHANNEL";
      case com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef.SellerTerminalScope
          ignored -> "SELLER_TERMINAL";
      case com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef.AgentScope ignored ->
          "AGENT";
    };
  }
}
