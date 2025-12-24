package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.UnregisterTerminalCommand;
import com.tchalanet.server.core.pos.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.pos.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class UnregisterPosDeviceCommandHandler implements CommandHandler<UnregisterTerminalCommand, Terminal> {

  private final TerminalReaderPort reader;
  private final TerminalWriterPort writer;
  private final Clock clock;

  @Override
  public Terminal handle(UnregisterTerminalCommand cmd) {
    var t = reader.findById(cmd.tenantId(), cmd.terminalId())
        .orElseThrow(() -> new IllegalStateException("Terminal not found"));
    var now = Instant.now(clock);
    // unregister = soft-delete; do not physically delete.
    return writer.save(t.unregister(cmd.actorId(), now));
  }
}
