package com.tchalanet.server.core.offlinesync.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.api.query.GetOfflineRiskDashboardQuery;
import com.tchalanet.server.core.offlinesync.api.query.OfflineRiskDashboardView;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSubmissionStatus;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOfflineRiskDashboardQueryHandler
    implements QueryHandler<GetOfflineRiskDashboardQuery, OfflineRiskDashboardView> {

  private final OfflineSubmissionReaderPort submissionReaderPort;

  @Override
  public OfflineRiskDashboardView handle(GetOfflineRiskDashboardQuery query) {
    return new OfflineRiskDashboardView(
        submissionReaderPort.countByTenantAndStatus(query.tenantId(), OfflineSubmissionStatus.SALES_REVIEW_REQUIRED),
        submissionReaderPort.countByTenantAndStatus(query.tenantId(), OfflineSubmissionStatus.TECHNICALLY_REJECTED),
        submissionReaderPort.countByTenantAndStatus(query.tenantId(), OfflineSubmissionStatus.SALES_REJECTED));
  }
}

