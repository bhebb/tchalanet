package com.tchalanet.server.core.pos.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.pos.application.query.model.GetPosDeviceByIdQuery;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetPosDeviceByIdQueryHandler implements QueryHandler<GetPosDeviceByIdQuery, Optional<Terminal>> {

  private final TerminalReaderPort readerPort;

  @Override
  public Optional<Terminal> handle(GetPosDeviceByIdQuery query) {
    return readerPort.findById(query.tenantId(), query.deviceId());
  }
}
