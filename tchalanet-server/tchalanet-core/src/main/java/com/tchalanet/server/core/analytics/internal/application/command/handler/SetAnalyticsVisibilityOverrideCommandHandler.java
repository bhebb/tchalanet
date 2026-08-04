package com.tchalanet.server.core.analytics.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.command.SetAnalyticsVisibilityOverrideCommand;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.api.model.AnalyticsVisibilityOverrideView;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsVisibilityOverrideEntity;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsVisibilityOverrideRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SetAnalyticsVisibilityOverrideCommandHandler
    implements CommandHandler<
        SetAnalyticsVisibilityOverrideCommand, AnalyticsVisibilityOverrideView> {

  private final AnalyticsVisibilityOverrideRepository repository;
  private final Clock clock;

  @TchTx
  @Override
  public AnalyticsVisibilityOverrideView handle(SetAnalyticsVisibilityOverrideCommand command) {
    AnalyticsTrustScope scope = command.scope();
    if (command.visible()) {
      repository.deleteExactScope(
          scope.type(),
          tenantValue(scope),
          sellerTerminalValue(scope),
          drawValue(scope),
          scope.from(),
          scope.to());
    } else {
      repository.save(
          AnalyticsVisibilityOverrideEntity.builder()
              .scopeType(scope.type())
              .tenantId(tenantValue(scope))
              .sellerTerminalId(sellerTerminalValue(scope))
              .drawId(drawValue(scope))
              .fromDate(scope.from())
              .toDate(scope.to())
              .reason(command.reason())
              .changedBy(command.changedBy())
              .changedAt(clock.instant())
              .build());
    }
    return new AnalyticsVisibilityOverrideView(
        scope, command.visible(), command.reason(), clock.instant());
  }

  private static java.util.UUID tenantValue(AnalyticsTrustScope scope) {
    return scope.tenantId() == null ? null : scope.tenantId().value();
  }

  private static java.util.UUID sellerTerminalValue(AnalyticsTrustScope scope) {
    return scope.sellerTerminalId() == null ? null : scope.sellerTerminalId().value();
  }

  private static java.util.UUID drawValue(AnalyticsTrustScope scope) {
    return scope.drawId() == null ? null : scope.drawId().value();
  }
}
