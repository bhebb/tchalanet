package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.NotAudited;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "offline_submission")
public class OfflineSubmissionJpaEntity extends BaseTenantEntity {

    @Column(name = "sync_batch_id")
    private UUID syncBatchId;

    @Column(name = "grant_id", nullable = false)
    private UUID grantId;

    @Column(name = "code_batch_id")
    private UUID codeBatchId;

    @Column(name = "offline_code")
    private String offlineCode;

    @Column(name = "client_submission_id", nullable = false)
    private String clientSubmissionId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "seller_user_id", nullable = false)
    private UUID sellerUserId;

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "outlet_id", nullable = false)
    private UUID outletId;

    @Column(name = "sales_session_id", nullable = false)
    private UUID salesSessionId;

    @Column(name = "client_sold_at", nullable = false)
    private Instant clientSoldAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "rejection_code")
    private String rejectionCode;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "draw_id", nullable = false)
    private UUID drawId;

    @Column(name = "total_stake_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalStakeAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "line_count", nullable = false)
    private Integer lineCount;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @NotAudited
    @Column(name = "signature")
    private String signature;

    @Column(name = "promotion_attempt_id")
    private UUID promotionAttemptId;

    @Column(name = "promotion_requested_at")
    private Instant promotionRequestedAt;

    @Column(name = "last_promotion_event_id")
    private UUID lastPromotionEventId;

    @Column(name = "created_ticket_id")
    private UUID createdTicketId;

    @NotAudited
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;
}
