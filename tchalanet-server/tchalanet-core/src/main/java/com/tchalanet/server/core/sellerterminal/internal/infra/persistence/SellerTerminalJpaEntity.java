package com.tchalanet.server.core.sellerterminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// No class-level @Audited: only control/financial fields carry @Audited(withModifiedFlag = true).
// Revisions are created only when those specific fields change, keeping PII and activity out of audit tables.
@Entity
@Table(
    name = "seller_terminal",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_seller_terminal_code", columnNames = {"tenant_id", "terminal_code"})
    }
)
@Getter
@Setter
public class SellerTerminalJpaEntity extends BaseTenantEntity {

    // ── Identity (PII — not audited) ──────────────────────────────────────────

    @Audited(withModifiedFlag = true)
    @Column(name = "terminal_code", nullable = false, length = 64)
    private String terminalCode;

    @Column(name = "first_name", length = 120)
    private String firstName;

    @Column(name = "last_name", length = 120)
    private String lastName;

    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    @Column(name = "phone_number", length = 64)
    private String phoneNumber;

    @Column(name = "address_id", columnDefinition = "uuid")
    private UUID addressId;

    // ── Control (audited) ─────────────────────────────────────────────────────

    @Audited(withModifiedFlag = true)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SellerTerminalStatus status = SellerTerminalStatus.PENDING;

    @Audited(withModifiedFlag = true)
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate = new BigDecimal("15.00");

    // ── Activity (not audited — high-churn) ──────────────────────────────────

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    // ── Block/disable state (audited) ─────────────────────────────────────────

    @Audited(withModifiedFlag = true)
    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Audited(withModifiedFlag = true)
    @Column(name = "blocked_by", columnDefinition = "uuid")
    private UUID blockedBy;

    @Audited(withModifiedFlag = true)
    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @Audited(withModifiedFlag = true)
    @Column(name = "disabled_at")
    private Instant disabledAt;
}
