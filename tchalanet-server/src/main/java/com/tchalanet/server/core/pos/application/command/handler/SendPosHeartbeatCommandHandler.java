package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.SendPosHeartbeatCommand;
import com.tchalanet.server.core.pos.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.pos.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class SendPosHeartbeatCommandHandler implements VoidCommandHandler<SendPosHeartbeatCommand> {

  private final TerminalReaderPort readerPort;
  private final TerminalWriterPort writerPort;
  private final ObjectMapper objectMapper;

  @Override
  public void handle(SendPosHeartbeatCommand command) {
    var terminalOpt = readerPort.findById(command.tenantId(), command.deviceId());
    if (terminalOpt.isEmpty()) {
      // TODO: log or throw
      return;
    }
    Terminal terminal = terminalOpt.get();
    // Merge extras into meta
    String metaDelta = null;
    try {
      var map = Map.of(
          "status", command.status(),
          "batteryPercent", command.batteryPercent(),
          "appVersion", command.appVersion(),
          "extras", command.extras()
      );
      metaDelta = objectMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      // ignore
    }
    terminal.heartbeat(command.lastSeenAt(), metaDelta);
    writerPort.save(terminal);
  }
}
