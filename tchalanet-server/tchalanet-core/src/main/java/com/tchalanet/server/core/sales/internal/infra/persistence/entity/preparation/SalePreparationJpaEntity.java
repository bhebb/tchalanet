package com.tchalanet.server.core.sales.internal.infra.persistence.entity.preparation;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Working trace of a prepared sale — the persisted ticket stays the financial
 * truth (see DOMAIN_SALES.md §11). Not Envers-audited: purged by retention job.
 */
@Entity
@Table(name = "sale_preparation")
@Getter
@Setter
public class SalePreparationJpaEntity extends BaseTenantEntity {

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "terminal_id")
    private UUID terminalId;

    @Column(name = "draw_id", nullable = false)
    private UUID drawId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SalePreparationStatus status = SalePreparationStatus.DRAFT;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "paid_lines_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> paidLinesJson;

    @Column(name = "promotion_decision_id")
    private UUID promotionDecisionId;

    @Column(name = "idempotency_key", length = 96)
    private String idempotencyKey;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;
}
