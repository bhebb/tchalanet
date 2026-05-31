package com.tchalanet.server.core.terminal.internal.application.command.handler.control;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.command.SetTerminalOperationalControlCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SetTerminalOperationalControlCommandHandler
    implements CommandHandler<SetTerminalOperationalControlCommand, Void> {

  private final TerminalReaderPort terminalReader;
  private final TerminalWriterPort terminalWriter;
  private final Clock clock;

  @Override
  @TchTx
  public Void handle(SetTerminalOperationalControlCommand cmd) {
    // Validates that the terminal belongs to cmd.tenantId() — throws if absent or cross-tenant.
    terminalReader.getRequired(cmd.tenantId(), cmd.terminalId());

    var now = clock.instant();

    switch (cmd.control()) {
      case SALES -> terminalWriter.setSalesBlocked(cmd.terminalId(), cmd.blocked(), cmd.reason(), now, cmd.performedBy());
      case PAYOUT -> terminalWriter.setPayoutBlocked(cmd.terminalId(), cmd.blocked(), cmd.reason(), now, cmd.performedBy());
      case OFFLINE -> terminalWriter.setOfflineBlocked(cmd.terminalId(), cmd.blocked(), cmd.reason(), now, cmd.performedBy());
    }

    return null;
  }
}
