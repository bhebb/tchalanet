package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.UpdateTerminalMetadataCommand;
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
public class UpdatePosDeviceMetadataCommandHandler
    implements CommandHandler<UpdateTerminalMetadataCommand, Terminal> {

  private final TerminalReaderPort reader;
  private final TerminalWriterPort writer;
  private final Clock clock;

  @Override
  public Terminal handle(UpdateTerminalMetadataCommand cmd) {
    var t =
        reader
            .findById(cmd.tenantId(), cmd.terminalId())
            .orElseThrow(() -> new IllegalStateException("Terminal not found"));
    var now = Instant.now(clock);

    var updated = t.mergeMetadata(cmd.metadataPatch(), now);

    if (cmd.heartbeatAlso()) {
      updated =
          new Terminal(
              updated.id(),
              updated.tenantId(),
              updated.outletId(),
              updated.state(),
              now,
              updated.meta(),
              updated.version(),
              updated.registeredAt(),
              updated.unregisteredAt(),
              updated.lockedAt(),
              updated.lockedBy(),
              updated.lockReason(),
              updated.deletedAt(),
              updated.label(),
              updated.inventoryTag());
    }

    return writer.save(updated);
  }
}
