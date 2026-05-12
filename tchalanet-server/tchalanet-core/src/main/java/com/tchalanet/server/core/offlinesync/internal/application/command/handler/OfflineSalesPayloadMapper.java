package com.tchalanet.server.core.offlinesync.internal.application.command.handler;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSaleSubmission;
import java.util.List;

@Deprecated(forRemoval = false)
public class OfflineSalesPayloadMapper {

  private final com.tchalanet.server.core.offlinesync.internal.application.service.OfflineSalesPayloadMapper delegate;

  public OfflineSalesPayloadMapper(com.tchalanet.server.core.offlinesync.internal.application.service.OfflineSalesPayloadMapper delegate) {
    this.delegate = delegate;
  }

  public List<OfflineSaleSubmission> toSalesCommand(OfflineBatchId batchId, List<OfflineSaleSubmission> submissions) {
    return delegate.mapSubmissions(batchId, submissions);
  }
}
