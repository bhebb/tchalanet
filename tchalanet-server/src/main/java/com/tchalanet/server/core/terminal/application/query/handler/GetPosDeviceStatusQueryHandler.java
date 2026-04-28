package com.tchalanet.server.core.terminal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.query.model.GetPosDeviceStatusQuery;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetPosDeviceStatusQueryHandler
    implements QueryHandler<GetPosDeviceStatusQuery, Map<String, Object>> {

  private final TerminalReaderPort readerPort;

  @Override
  public Map<String, Object> handle(GetPosDeviceStatusQuery query) {
    var terminalOpt = readerPort.findById(query.tenantId(), query.deviceId());
    if (terminalOpt.isEmpty()) {
      return Map.of("error", "Device not found");
    }
    var terminal = terminalOpt.get();
    return Map.of(
        "state", terminal.state().name(),
        "lastSeen", terminal.lastSeen());
  }
}
