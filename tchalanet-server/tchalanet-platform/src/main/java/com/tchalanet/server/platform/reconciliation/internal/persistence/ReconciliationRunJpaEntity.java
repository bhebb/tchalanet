package com.tchalanet.server.platform.reconciliation.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for reconciliation run
 * Table: reconciliation_run
 */
@Entity
@Table(name = "reconciliation_run")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRunJpaEntity extends BaseEntity {

    @Column(name = "tenant_id", columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "scope", length = 128, nullable = false)
    private String scope;

    @Column(name = "business_date")
    private LocalDate businessDate;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "triggered_by", length = 128)
    private String triggeredBy;

    @Column(name = "triggered_by_user_id", columnDefinition = "uuid")
    private UUID triggeredByUserId;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "summary_json", columnDefinition = "jsonb")
    private String summaryJson;
}

