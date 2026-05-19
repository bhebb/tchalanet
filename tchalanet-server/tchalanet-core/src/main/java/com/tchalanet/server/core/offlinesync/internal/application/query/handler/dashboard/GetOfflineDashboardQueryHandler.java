package com.tchalanet.server.core.offlinesync.internal.application.query.handler.dashboard;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.query.dashboard.GetOfflineDashboardQuery;
import com.tchalanet.server.core.offlinesync.api.query.dashboard.OfflineDashboardView;
import com.tchalanet.server.core.offlinesync.api.model.submission.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSubmissionJpaRepository;
import lombok.RequiredArgsConstructor;

/**
 * Aggregated counters for the offline ops dashboard.
 *
 * <p>TODO Phase E3+: add counters for active grants and last-24h breakdown — currently
 * only the easy {@code NEEDS_ADMIN_REVIEW} count is wired through the JPA repository.
 */
@UseCase
@RequiredArgsConstructor
public class GetOfflineDashboardQueryHandler
    implements QueryHandler<GetOfflineDashboardQuery, OfflineDashboardView> {

    private final OfflineSubmissionJpaRepository submissionRepo;

    @Override
    @TchTx(readOnly = true)
    public OfflineDashboardView handle(GetOfflineDashboardQuery query) {
        long pendingReview = submissionRepo.countByTenantIdAndStatus(
            query.tenantId().value(), OfflineSubmissionStatus.NEEDS_ADMIN_REVIEW.name());
        return new OfflineDashboardView(
            0,                            // activeGrantCount — TODO
            (int) pendingReview,
            0,                            // last24hAcceptedSubmissionCount — TODO
            0,                            // last24hRejectedSubmissionCount — TODO
            0                             // stuckSubmissionCount — TODO
        );
    }
}
