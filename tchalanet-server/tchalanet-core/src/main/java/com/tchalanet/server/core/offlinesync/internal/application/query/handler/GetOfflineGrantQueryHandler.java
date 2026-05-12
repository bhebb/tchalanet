package com.tchalanet.server.core.offlinesync.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.application.query.model.GetOfflineGrantQuery;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOfflineGrantQueryHandler implements QueryHandler<GetOfflineGrantQuery, OfflineSalesGrant> {

  private final OfflineGrantReaderPort grantReaderPort;

  @Override
  public OfflineSalesGrant handle(GetOfflineGrantQuery query) {
    return grantReaderPort.findById(query.grantId()).orElse(null);
  }
}

