package com.tchalanet.server.core.analytics.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.command.ReconcileAnalyticsCommand;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationMode;
import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationResult;
import com.tchalanet.server.core.analytics.internal.application.service.AnalyticsReconciliationService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReconcileAnalyticsCommandHandler
    implements CommandHandler<ReconcileAnalyticsCommand, AnalyticsReconciliationResult> {

  private final AnalyticsReconciliationService service;

  @Override
  @TchTx
  public AnalyticsReconciliationResult handle(ReconcileAnalyticsCommand command) {
    return command.mode() == AnalyticsReconciliationMode.VALIDATE
        ? service.validate(command)
        : service.rebuildAndValidate(command);
  }
}
