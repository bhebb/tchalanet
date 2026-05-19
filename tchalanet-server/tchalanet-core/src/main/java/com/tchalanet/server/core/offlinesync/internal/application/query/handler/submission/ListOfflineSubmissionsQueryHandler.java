package com.tchalanet.server.core.offlinesync.internal.application.query.handler.submission;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.query.submission.ListOfflineSubmissionsQuery;
import com.tchalanet.server.core.offlinesync.api.query.submission.OfflineSubmissionView;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.query.mapper.OfflineSubmissionViewMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListOfflineSubmissionsQueryHandler
    implements QueryHandler<ListOfflineSubmissionsQuery, List<OfflineSubmissionView>> {

    private final OfflineSubmissionReaderPort submissionReader;

    @Override
    @TchTx(readOnly = true)
    public List<OfflineSubmissionView> handle(ListOfflineSubmissionsQuery query) {
        return submissionReader
            .listForSeller(query.tenantId(), query.sellerUserId(), query.limit())
            .stream()
            .filter(s -> query.statuses() == null || query.statuses().isEmpty()
                || query.statuses().contains(s.status()))
            .map(OfflineSubmissionViewMapper::toView)
            .toList();
    }
}
