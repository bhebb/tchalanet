package com.tchalanet.server.core.offlinesync.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.query.model.GetOfflineQueueSizeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetOfflineQueueSizeQueryHandler implements QueryHandler<GetOfflineQueueSizeQuery, Integer> {

  @Override
  public Integer handle(GetOfflineQueueSizeQuery query) {
    // TODO: return queue size
    throw new UnsupportedOperationException("GetOfflineQueueSizeQueryHandler not implemented yet");
  }
}

