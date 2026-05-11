package com.tchalanet.server.core.offlinesync.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Audited
@Table(name = "offline_batch")
public class OfflineBatchJpaEntity {
    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;
    @Column(name = "outlet_id", nullable = false)
    private UUID outletId;
    @Column(name = "seller_user_id", nullable = false)
    private UUID sellerUserId;
    @Column(name = "sales_session_id", nullable = false)
    private UUID salesSessionId;
    @Column(name = "grant_id", nullable = false)
    private UUID grantId;
    @Column(name = "code_batch_id", nullable = false)
    private UUID codeBatchId;
    @Column(name = "client_batch_id", nullable = false)
    private String clientBatchId;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "status", nullable = false)
    private String status;
    @Column(name = "ticket_count", nullable = false)
    private Integer ticketCount;
    @Column(name = "technical_reject_count", nullable = false)
    private Integer technicalRejectCount;
    @Column(name = "sales_accept_count", nullable = false)
    private Integer salesAcceptCount;
    @Column(name = "sales_reject_count", nullable = false)
    private Integer salesRejectCount;
    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;
    @Version
    private Long version;
}
