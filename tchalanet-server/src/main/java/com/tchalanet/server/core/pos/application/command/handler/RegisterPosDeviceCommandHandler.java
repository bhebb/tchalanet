package com.tchalanet.server.core.pos.application.command.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
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
  private final ObjectMapper objectMapper;

  @Override
  public UUID handle(RegisterPosDeviceCommand command) {
    UUID deviceId = command.deviceId() != null ? command.deviceId() : UUID.randomUUID();
    String meta;
    try {
      meta = objectMapper.writeValueAsString(command.capabilities());
    } catch (JsonProcessingException e) {
      meta = "{}";
    }
    Terminal terminal =
        new Terminal(
            deviceId,
            command.tenantId(),
            command.outletId(),
            Terminal.TerminalState.INACTIVE,
            null,
            meta,
            0L,
            null, // registeredAt will be set by register
            null, // unregisteredAt
            null, // lockedAt
            null, // lockedBy
            null, // lockReason
            null // deletedAt
            );
    var now = Instant.now();
    terminal =
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
            null);
    writerPort.save(terminal);
    return deviceId;
  }
}
