package com.tchalanet.server.core.session.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "pos_session_totals")
@Getter
@Setter
@Audited
public class PosSessionTotalsJpaEntity {

    @Id
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId // <-- session_id = id de PosSession
    @JoinColumn(name = "session_id", nullable = false)
    private PosSessionJpaEntity session;

    // On garde tenant_id comme colonne (utile pour filtres/RLS/index)
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "total_tickets", nullable = false)
    private long totalTickets;

    @Column(name = "total_stake", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalStake;

    @Column(name = "total_payout", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPayout;

    @Column(name = "gross_margin", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossMargin;

    // audit
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
