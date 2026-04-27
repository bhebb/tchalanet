package com.tchalanet.server.features.ops.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowCommand;
import com.tchalanet.server.features.ops.application.command.model.RefreshExternalResultsWindowCommand;
import com.tchalanet.server.features.ops.application.command.model.RefreshExternalResultsWindowResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RefreshExternalResultsWindowCommandHandler
    implements CommandHandler<RefreshExternalResultsWindowCommand, RefreshExternalResultsWindowResult> {

  private final CommandBus commandBus;

  @Override
  public RefreshExternalResultsWindowResult handle(RefreshExternalResultsWindowCommand cmd) {
    log.info("Refreshing external results window: baseDate={}, daysBack={}, tenantId={}",
        cmd.baseDate(), cmd.daysBack(), cmd.tenantId());

    // 1. Fetch
    var fetchRes = commandBus.send(new FetchExternalResultsWindowCommand(
        cmd.tenantId(),
        cmd.baseDate(),
        cmd.daysBack(),
        cmd.slotKeys(),
        cmd.force(),
        cmd.dryRun(),
        cmd.maxSlots(),
        cmd.reason()
    ));

    // 2. Apply (only if fetch was successful or had some results)
    var applyRes = commandBus.send(new ApplyExternalResultsWindowCommand(
        cmd.tenantId(),
        cmd.baseDate(),
        cmd.daysBack(),
        cmd.slotKeys(),
        cmd.force(),
        cmd.dryRun(),
        cmd.maxSlots(),
        cmd.reason()
    ));

    return RefreshExternalResultsWindowResult.from(fetchRes, applyRes);
  }
}
