package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Getter
@Setter
@Entity
@Audited
@Table(name = "offline_submission_decision")
public class OfflineSubmissionDecisionJpaEntity extends BaseTenantEntity {

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "decision_type", nullable = false)
    private String decisionType;

    @Column(name = "decided_by", nullable = false)
    private UUID decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "dry_run", nullable = false)
    private boolean dryRun;

    @NotAudited
    @Column(name = "report_json", columnDefinition = "jsonb")
    private String reportJson;
}
