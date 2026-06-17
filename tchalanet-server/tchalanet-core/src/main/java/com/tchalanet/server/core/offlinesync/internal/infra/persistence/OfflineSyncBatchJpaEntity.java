package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.NotAudited;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "offline_sync_batch")
public class OfflineSyncBatchJpaEntity extends BaseTenantEntity {

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "outlet_id", nullable = false)
    private UUID outletId;

    @Column(name = "seller_user_id", nullable = false)
    private UUID sellerUserId;

    @Column(name = "sales_session_id", nullable = false)
    private UUID salesSessionId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "grant_id", nullable = false)
    private UUID grantId;

    @Column(name = "code_batch_id")
    private UUID codeBatchId;

    @Column(name = "client_batch_id", nullable = false)
    private String clientBatchId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "submission_count", nullable = false)
    private Integer submissionCount;

    @Column(name = "technical_reject_count", nullable = false)
    private Integer technicalRejectCount;

    @Column(name = "sales_accept_count", nullable = false)
    private Integer salesAcceptCount;

    @Column(name = "sales_reject_count", nullable = false)
    private Integer salesRejectCount;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @NotAudited
    @Column(name = "raw_manifest", columnDefinition = "jsonb")
    private String rawManifest;
}
