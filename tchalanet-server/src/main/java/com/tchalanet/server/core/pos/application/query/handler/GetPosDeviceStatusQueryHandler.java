package com.tchalanet.server.core.pos.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.query.model.GetPosDeviceStatusQuery;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetPosDeviceStatusQueryHandler implements QueryHandler<GetPosDeviceStatusQuery, Map<String,Object>> {

  @Override
  public Map<String,Object> handle(GetPosDeviceStatusQuery query) {
    // TODO: return status details
    throw new UnsupportedOperationException("GetPosDeviceStatusQueryHandler not implemented yet");
  }
}

