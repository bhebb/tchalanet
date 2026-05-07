package com.tchalanet.server.core.terminal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.application.command.model.SendTerminalHeartbeatCommand;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import lombok.RequiredArgsConstructor;

/**
 * Heartbeat handler. No event published per heartbeat (too noisy). Side effect: lastSeen is
 * refreshed and syncState becomes ONLINE.
 */
@UseCase
@RequiredArgsConstructor
public class SendTerminalHeartbeatCommandHandler
    implements VoidCommandHandler<SendTerminalHeartbeatCommand> {

  private final TerminalReaderPort reader;
  private final TerminalWriterPort writer;

  @Override
  @TchTx
  public void handle(SendTerminalHeartbeatCommand cmd) {
    Terminal t = reader.getRequired(cmd.tenantId(), cmd.terminalId());
    writer.save(t.heartbeat(cmd.occurredAt()));
  }
}
