package com.tchalanet.server.core.offlinesync.internal.application.query.handler.submission;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.query.submission.GetOfflineSubmissionQuery;
import com.tchalanet.server.core.offlinesync.api.query.submission.OfflineSubmissionView;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.query.mapper.OfflineSubmissionViewMapper;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOfflineSubmissionQueryHandler
    implements QueryHandler<GetOfflineSubmissionQuery, OfflineSubmissionView> {

    private final OfflineSubmissionReaderPort submissionReader;

    @Override
    @TchTx(readOnly = true)
    public OfflineSubmissionView handle(GetOfflineSubmissionQuery query) {
        return OfflineSubmissionViewMapper.toView(submissionReader.getRequired(query.submissionId()));
    }
}
