package com.tchalanet.server.core.payout.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payout")
@Getter
@Setter
public class PayoutJpaEntity extends BaseTenantEntity {

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;

    @Column(name = "draw_id", updatable = false)
    private UUID drawId;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private Long amountCents;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency = "HTG";

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private PayoutClaimStatus status;

    @Column(name = "source", length = 32, updatable = false)
    @Enumerated(EnumType.STRING)
    private com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimSource source;

    @Column(name = "source_event_id", updatable = false)
    private UUID sourceEventId;

    @Column(name = "selling_outlet_id", updatable = false)
    private UUID sellingOutletId;

    @Column(name = "selling_session_id", updatable = false)
    private UUID sellingSessionId;

    @Column(name = "opened_at", updatable = false)
    private Instant openedAt;

    @Column(name = "paying_outlet_id")
    private UUID payingOutletId;

    @Column(name = "paying_session_id")
    private UUID payingSessionId;

    @Column(name = "paying_terminal_id")
    private UUID payingTerminalId;

    @Column(name = "paid_by")
    private UUID paidBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "blocked_by")
    private UUID blockedBy;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "block_reason")
    private String blockReason;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "reversed_by")
    private UUID reversedBy;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reverse_reason")
    private String reverseReason;
}
