package com.tchalanet.server.platform.reconciliation.internal.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for reconciliation repair action
 * Table: reconciliation_repair_action
 */
@Entity
@Table(name = "reconciliation_repair_action")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRepairActionJpaEntity extends BaseTenantEntity {

    @Column(name = "anomaly_id", columnDefinition = "uuid", nullable = false)
    private UUID anomalyId;

    @Column(name = "run_id", columnDefinition = "uuid", nullable = false)
    private UUID runId;

    @Column(name = "action_type", length = 128, nullable = false)
    private String actionType;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "command_name", length = 256)
    private String commandName;

    @Column(name = "command_payload_json", columnDefinition = "jsonb")
    private String commandPayloadJson;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "executed_by", length = 128)
    private String executedBy;

    @Column(name = "failure_message", length = 1024)
    private String failureMessage;
}


