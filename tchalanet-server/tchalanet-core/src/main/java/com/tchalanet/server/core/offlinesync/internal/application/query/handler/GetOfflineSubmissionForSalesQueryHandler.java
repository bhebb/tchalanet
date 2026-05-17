package com.tchalanet.server.core.offlinesync.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionForSalesView;
import com.tchalanet.server.core.offlinesync.api.query.GetOfflineSubmissionForSalesQuery;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionQueryReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOfflineSubmissionForSalesQueryHandler
    implements QueryHandler<GetOfflineSubmissionForSalesQuery, OfflineSubmissionForSalesView> {

    private final OfflineSubmissionQueryReaderPort reader;

    @Override
    public OfflineSubmissionForSalesView handle(GetOfflineSubmissionForSalesQuery query) {
        return reader.getForSalesById(query.tenantId(), query.submissionId());
    }
}
