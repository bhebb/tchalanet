package com.tchalanet.server.core.limitpolicy.infra.persistence.entity;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.enums.ScopeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "draw_exposure")
@Getter
@Setter
public class DrawExposureJpaEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "draw_id", nullable = false)
    private UUID drawId;

    @Column(name = "scope_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ScopeType scopeType;

    @Column(name = "scope_id", nullable = false)
    private UUID scopeId;

    @Column(name = "bet_type")
    @Enumerated(EnumType.STRING)
    private BetType betType;

    @Column(name = "selection_key")
    private String selectionKey;

    @Column(name = "stake_total", nullable = false)
    private BigDecimal stakeTotal = BigDecimal.ZERO;

    @Column(name = "sales_count", nullable = false)
    private long salesCount = 0;

    @Column(name = "potential_payout_total", nullable = false)
    private BigDecimal potentialPayoutTotal = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Composite key
    @EmbeddedId
    private DrawExposureId id;

    @Setter
    @Getter
    public static class DrawExposureId implements java.io.Serializable {
        private UUID tenantId;
        private UUID drawId;
        private ScopeType scopeType;
        private UUID scopeId;
        private BetType betType;
        private String selectionKey;
    }
}
