package com.tchalanet.server.core.offlinesync.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.query.model.GetOfflineQueueDetailsQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetOfflineQueueDetailsQueryHandler implements QueryHandler<GetOfflineQueueDetailsQuery, List<Object>> {

  @Override
  public List<Object> handle(GetOfflineQueueDetailsQuery query) {
    // TODO: return paginated queue details
    throw new UnsupportedOperationException("GetOfflineQueueDetailsQueryHandler not implemented yet");
  }
}

