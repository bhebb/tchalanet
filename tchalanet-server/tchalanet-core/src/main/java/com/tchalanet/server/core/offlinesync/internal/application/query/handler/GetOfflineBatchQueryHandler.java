package com.tchalanet.server.core.offlinesync.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineBatchReaderPort;
import com.tchalanet.server.core.offlinesync.api.query.GetOfflineBatchQuery;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineBatch;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOfflineBatchQueryHandler implements QueryHandler<GetOfflineBatchQuery, OfflineBatch> {

  private final OfflineBatchReaderPort batchReaderPort;

  @Override
  public OfflineBatch handle(GetOfflineBatchQuery query) {
    return batchReaderPort.findById(query.batchId()).orElse(null);
  }
}

