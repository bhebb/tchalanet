package com.tchalanet.server.core.reconciliation.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationAnomalyStatus;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationAnomalyType;
import com.tchalanet.server.core.reconciliation.internal.domain.model.ReconciliationSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "reconciliation_anomaly",
    indexes = {
        @Index(name = "idx_reconciliation_anomaly_run", columnList = "tenant_id,run_id"),
        @Index(name = "idx_reconciliation_anomaly_status_severity", columnList = "tenant_id,status,severity")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_reconciliation_anomaly_fingerprint",
            columnNames = {"tenant_id", "fingerprint"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationAnomalyJpaEntity extends BaseTenantEntity {

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private ReconciliationSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false, length = 96)
    private ReconciliationAnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReconciliationAnomalyStatus status;

    @Column(name = "fingerprint", nullable = false, length = 256)
    private String fingerprint;

    @Column(name = "draw_id")
    private UUID drawId;

    @Column(name = "draw_channel_id")
    private UUID drawChannelId;

    @Column(name = "draw_result_id")
    private UUID drawResultId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "ticket_code", length = 96)
    private String ticketCode;

    @Column(name = "public_code", length = 96)
    private String publicCode;

    @Column(name = "display_code", length = 96)
    private String displayCode;

    @Column(name = "payout_claim_id")
    private UUID payoutClaimId;

    @Column(name = "payout_payment_id")
    private UUID payoutPaymentId;

    @Column(name = "expected_status", length = 64)
    private String expectedStatus;

    @Column(name = "actual_status", length = 64)
    private String actualStatus;

    @Column(name = "expected_amount", precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "actual_amount", precision = 19, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "details_json", nullable = false, columnDefinition = "jsonb")
    private String detailsJson;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
