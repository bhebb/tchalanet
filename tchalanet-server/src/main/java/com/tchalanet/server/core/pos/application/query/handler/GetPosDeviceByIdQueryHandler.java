package com.tchalanet.server.core.pos.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.query.model.GetPosDeviceByIdQuery;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetPosDeviceByIdQueryHandler implements QueryHandler<GetPosDeviceByIdQuery, Optional<Object>> {

  @Override
  public Optional<Object> handle(GetPosDeviceByIdQuery query) {
    // TODO: return device DTO
    throw new UnsupportedOperationException("GetPosDeviceByIdQueryHandler not implemented yet");
  }
}

