package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pos.application.command.model.RegisterPosDeviceCommand;
import com.tchalanet.server.core.pos.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class RegisterPosDeviceCommandHandler
    implements CommandHandler<RegisterPosDeviceCommand, UUID> {

  private final TerminalWriterPort writerPort;
  private final JsonUtils jsonUtils;

  @Override
  public UUID handle(RegisterPosDeviceCommand command) {
    UUID deviceId = command.deviceId() != null ? command.deviceId() : UUID.randomUUID();
    String meta;
    try {
      meta = jsonUtils.toJson(command.capabilities());
    } catch (Exception e) {
      meta = "{}";
    }
    var now = Instant.now();
    Terminal terminal =
        new Terminal(
            deviceId,
            command.tenantId(),
            command.outletId(),
            Terminal.TerminalState.ACTIVE,
            now,
            meta,
            0L,
            now, // registeredAt
            null,
            null,
            null,
            null,
            null,
            command.label(),
            null);
    writerPort.save(terminal);
    return deviceId;
  }
}
