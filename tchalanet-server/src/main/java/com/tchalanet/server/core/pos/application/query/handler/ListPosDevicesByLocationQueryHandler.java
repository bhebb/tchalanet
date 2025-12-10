package com.tchalanet.server.core.pos.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.query.model.ListPosDevicesByLocationQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ListPosDevicesByLocationQueryHandler implements QueryHandler<ListPosDevicesByLocationQuery, List<Object>> {

  @Override
  public List<Object> handle(ListPosDevicesByLocationQuery query) {
    // TODO: return list of devices DTO
    throw new UnsupportedOperationException("ListPosDevicesByLocationQueryHandler not implemented yet");
  }
}

