package com.tchalanet.server.core.offlinesync.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.api.query.GetOfflineGrantQuery;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOfflineGrantQueryHandler implements QueryHandler<GetOfflineGrantQuery, OfflineSalesGrant> {

  private final OfflineGrantReaderPort grantReaderPort;

  @Override
  public OfflineSalesGrant handle(GetOfflineGrantQuery query) {
    var grant = grantReaderPort.findById(query.grantId()).orElse(null);
    if (grant == null || !query.tenantId().equals(grant.tenantId())) {
      return null;
    }
    return grant;
  }
}
