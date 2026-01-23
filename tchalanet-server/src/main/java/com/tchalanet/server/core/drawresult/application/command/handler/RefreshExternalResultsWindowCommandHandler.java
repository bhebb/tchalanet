package com.tchalanet.server.catalog.drawresult.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.catalog.drawresult.application.command.model.FetchExternalResultsWindowCommand;
import com.tchalanet.server.catalog.drawresult.application.command.model.RefreshExternalResultsWindowCommand;
import com.tchalanet.server.catalog.drawresult.application.command.model.RefreshExternalResultsWindowResult;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RefreshExternalResultsWindowCommandHandler
    implements CommandHandler<
        RefreshExternalResultsWindowCommand, RefreshExternalResultsWindowResult> {

  private final CommandBus bus;

  @Override
  public RefreshExternalResultsWindowResult handle(RefreshExternalResultsWindowCommand cmd) {
    Objects.requireNonNull(cmd, "command is required");

    var fetch =
        bus.send(
            new FetchExternalResultsWindowCommand(
                cmd.tenantId(),
                cmd.baseDate(),
                cmd.daysBack(),
                cmd.slotKeys(),
                cmd.force(),
                cmd.dryRun(),
                cmd.maxSlots()));

    var apply =
        bus.send(
            new ApplyExternalResultsWindowCommand(
                cmd.tenantId(),
                cmd.baseDate(),
                cmd.daysBack(),
                cmd.slotKeys(),
                cmd.force(),
                cmd.dryRun(),
                cmd.maxSlots()));

    return RefreshExternalResultsWindowResult.from(fetch, apply);
  }
}
