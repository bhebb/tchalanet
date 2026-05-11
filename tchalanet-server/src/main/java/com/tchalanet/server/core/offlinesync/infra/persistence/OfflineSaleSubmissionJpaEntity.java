package com.tchalanet.server.core.offlinesync.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Audited
@Table(name = "offline_sale_submission")
public class OfflineSaleSubmissionJpaEntity extends BaseTenantEntity {

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "grant_id", nullable = false)
    private UUID grantId;

    @Column(name = "code_batch_id", nullable = false)
    private UUID codeBatchId;

    @Column(name = "offline_code", nullable = false)
    private String offlineCode;

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "outlet_id", nullable = false)
    private UUID outletId;

    @Column(name = "seller_user_id", nullable = false)
    private UUID sellerUserId;

    @Column(name = "sales_session_id", nullable = false)
    private UUID salesSessionId;

    @Column(name = "client_ticket_id", nullable = false)
    private String clientTicketId;

    @Column(name = "local_sequence", nullable = false)
    private Long localSequence;

    @Column(name = "created_at_device", nullable = false)
    private Instant createdAtDevice;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @Column(name = "signature", nullable = false)
    private String signature;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "technical_reject_reason")
    private String technicalRejectReason;

    @Column(name = "sales_decision")
    private String salesDecision;

    @Column(name = "sales_reject_reason")
    private String salesRejectReason;

    @Column(name = "sales_ticket_id")
    private UUID salesTicketId;

    @Column(name = "risk_flags_json", columnDefinition = "jsonb")
    private String riskFlagsJson;
}
