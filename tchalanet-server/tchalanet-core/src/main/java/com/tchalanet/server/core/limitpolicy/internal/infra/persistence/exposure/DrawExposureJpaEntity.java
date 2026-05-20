package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.limitpolicy.api.ScopeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "draw_exposure",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_draw_exposure_key",
        columnNames = {"tenant_id", "draw_id", "scope_type", "scope_id", "bet_type", "selection_key"}
    )
)
@Getter
@Setter
public class DrawExposureJpaEntity extends BaseTenantEntity {

    // id + tenant_id come from BaseTenantEntity (id uuid, tenant_id uuid)
    // Ensure BaseTenantEntity maps `id` to column "id" and `tenantId` to "tenant_id".

    @Column(name = "draw_id", nullable = false)
    private UUID drawId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 16)
    private ScopeType scopeType;

    @Column(name = "scope_id", nullable = false)
    private UUID scopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bet_type", nullable = false, length = 32)
    private BetType betType;

    @Column(name = "selection_key", nullable = false, length = 64)
    private String selectionKey;

    @Column(name = "stake_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal stakeTotal = BigDecimal.ZERO;

    @Column(name = "sales_count", nullable = false)
    private long salesCount = 0L;

    @Column(name = "potential_payout_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal potentialPayoutTotal = BigDecimal.ZERO;

    @Column(name = "last_event_id")
    private UUID lastEventId;

    @Column(name = "last_event_at")
    private Instant lastEventAt;
}
