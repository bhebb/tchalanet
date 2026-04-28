package com.tchalanet.server.core.terminal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.application.command.model.UnlockTerminalCommand;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class UnlockPosDeviceCommandHandler
    implements CommandHandler<UnlockTerminalCommand, Terminal> {

  private final TerminalReaderPort reader;
  private final TerminalWriterPort writer;
  private final Clock clock;

  @Override
  public Terminal handle(UnlockTerminalCommand cmd) {
    var t =
        reader
            .findById(cmd.tenantId(), cmd.terminalId())
            .orElseThrow(() -> new IllegalStateException("Terminal not found"));
    var now = Instant.now(clock);
    return writer.save(t.unlock(cmd.actorId(), now));
  }
}
