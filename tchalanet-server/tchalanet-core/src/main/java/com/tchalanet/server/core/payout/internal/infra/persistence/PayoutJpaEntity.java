package com.tchalanet.server.core.payout.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "payout")
@Audited
@Getter
@Setter
public class PayoutJpaEntity extends BaseTenantEntity {

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private Long amountCents;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency = "HTG";

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private PayoutStatus status;

    @Column(name = "selling_outlet_id")
    private UUID sellingOutletId;

    @Column(name = "selling_session_id")
    private UUID sellingSessionId;

    @Column(name = "paying_outlet_id")
    private UUID payingOutletId;

    @Column(name = "paying_session_id")
    private UUID payingSessionId;

    @Column(name = "paying_terminal_id")
    private UUID payingTerminalId;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejected_reason")
    private String rejectedReason;

    @Column(name = "paid_by")
    private UUID paidBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "reason")
    private String reason;
}
