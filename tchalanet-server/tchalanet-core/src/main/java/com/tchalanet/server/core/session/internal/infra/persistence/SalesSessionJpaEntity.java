package com.tchalanet.server.core.session.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.session.internal.domain.model.SalesSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sales_session")
@Getter
@Setter
public class SalesSessionJpaEntity extends BaseTenantEntity {

    @Column(name = "outlet_id", nullable = false, updatable = false)
    private UUID outletId;

    @Column(name = "terminal_id", nullable = false, updatable = false)
    private UUID terminalId;

    @Column(name = "opened_by", nullable = false, updatable = false)
    private UUID openedBy;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private SalesSessionStatus status;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "close_reason")
    private String closeReason;

    @Column(name = "opening_float_cents")
    private Long openingFloatCents;

    @Column(name = "expected_closing_amount_cents")
    private Long expectedClosingAmountCents;

    @Column(name = "declared_closing_amount_cents")
    private Long declaredClosingAmountCents;

    @Column(name = "variance_cents")
    private Long varianceCents;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "finalized_by")
    private UUID finalizedBy;

    @Column(name = "finalize_reason")
    private String finalizeReason;
}
