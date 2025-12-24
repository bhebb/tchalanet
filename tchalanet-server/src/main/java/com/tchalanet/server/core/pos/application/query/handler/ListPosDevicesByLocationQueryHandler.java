package com.tchalanet.server.core.pos.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.pos.application.query.model.ListPosDevicesByLocationQuery;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ListPosDevicesByLocationQueryHandler implements QueryHandler<ListPosDevicesByLocationQuery, List<Terminal>> {

  private final TerminalReaderPort readerPort;

  @Override
  public List<Terminal> handle(ListPosDevicesByLocationQuery query) {
    // For simplicity, fetch all, no paging
    return readerPort.listByOutlet(query.tenantId(), query.outletId(), PageRequest.of(0, Integer.MAX_VALUE));
  }
}
