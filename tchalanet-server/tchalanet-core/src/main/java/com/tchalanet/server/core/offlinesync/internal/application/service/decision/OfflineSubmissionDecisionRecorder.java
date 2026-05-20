package com.tchalanet.server.core.offlinesync.internal.application.service.decision;

import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSubmissionDecisionJpaEntity;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSubmissionDecisionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Persists an audit trail row in {@code offline_submission_decision} every time an admin
 * approves, rejects, or replays an offline submission.
 */
@Component
@RequiredArgsConstructor
public class OfflineSubmissionDecisionRecorder {

    public enum DecisionType { APPROVE, REJECT, REPLAY }

    private final OfflineSubmissionDecisionJpaRepository repo;
    private final IdGenerator idGenerator;

    public void record(
        TenantId tenantId,
        OfflineSubmissionId submissionId,
        UserId decidedBy,
        DecisionType type,
        String reason,
        Instant decidedAt,
        boolean dryRun,
        String reportJson
    ) {
        var e = new OfflineSubmissionDecisionJpaEntity();
        e.setId(idGenerator.newUuid());
        e.setTenantId(tenantId.value());
        e.setSubmissionId(submissionId.value());
        e.setDecidedBy(decidedBy.value());
        e.setDecisionType(type.name());
        e.setReason(reason == null ? "" : reason);
        e.setDecidedAt(decidedAt);
        e.setDryRun(dryRun);
        e.setReportJson(reportJson);
        repo.save(e);
    }
}
