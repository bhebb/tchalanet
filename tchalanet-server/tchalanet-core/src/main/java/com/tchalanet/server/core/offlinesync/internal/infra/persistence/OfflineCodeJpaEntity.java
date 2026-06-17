package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "offline_code")
public class OfflineCodeJpaEntity extends BaseTenantEntity {

    @Column(name = "code_batch_id", nullable = false)
    private UUID codeBatchId;

    @Column(name = "grant_id")
    private UUID grantId;

    @Column(name = "offline_submission_id")
    private UUID offlineSubmissionId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
