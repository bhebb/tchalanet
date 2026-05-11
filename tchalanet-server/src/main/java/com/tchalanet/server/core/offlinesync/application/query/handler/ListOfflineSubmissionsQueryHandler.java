package com.tchalanet.server.core.offlinesync.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.application.query.model.ListOfflineSubmissionsQuery;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSaleSubmission;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListOfflineSubmissionsQueryHandler
    implements QueryHandler<ListOfflineSubmissionsQuery, List<OfflineSaleSubmission>> {

  private final OfflineSubmissionReaderPort submissionReaderPort;

  @Override
  public List<OfflineSaleSubmission> handle(ListOfflineSubmissionsQuery query) {
    return submissionReaderPort.listByBatch(query.batchId(), query.status());
  }
}

