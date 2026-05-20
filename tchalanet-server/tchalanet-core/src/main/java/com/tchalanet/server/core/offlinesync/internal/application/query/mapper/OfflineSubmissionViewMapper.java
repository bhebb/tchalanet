package com.tchalanet.server.core.offlinesync.internal.application.query.mapper;

import com.tchalanet.server.core.offlinesync.api.query.submission.OfflineSubmissionView;
import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.OfflineSubmission;

public final class OfflineSubmissionViewMapper {

    private OfflineSubmissionViewMapper() {}

    public static OfflineSubmissionView toView(OfflineSubmission submission) {
        return new OfflineSubmissionView(
            submission.identity().id(),
            submission.identity().grantId(),
            submission.context().sellerUserId(),
            submission.lifecycle().status(),
            submission.payload().clientSoldAt(),
            submission.lifecycle().receivedAt(),
            submission.lifecycle().processedAt(),
            submission.payload().totalStakeAmount(),
            submission.payload().lineCount(),
            submission.lifecycle().rejectionCode(),
            submission.lifecycle().rejectionReason(),
            submission.promotion().createdTicketId()
        );
    }
}
