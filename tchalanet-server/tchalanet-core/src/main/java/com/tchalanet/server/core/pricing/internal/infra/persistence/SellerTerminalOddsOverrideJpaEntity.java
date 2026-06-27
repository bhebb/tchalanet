package com.tchalanet.server.core.pricing.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "seller_terminal_pricing_odds_override",
    indexes = {
        @Index(name = "idx_st_pricing_odds_tenant",          columnList = "tenant_id"),
        @Index(name = "idx_st_pricing_odds_seller_terminal", columnList = "seller_terminal_id")
    }
)
@Getter
@Setter
public class SellerTerminalOddsOverrideJpaEntity extends BaseTenantEntity {

    @Column(name = "seller_terminal_id", nullable = false)
    private UUID sellerTerminalId;

    @Column(name = "game_code", nullable = false, length = 32)
    private String gameCode;

    @Column(name = "bet_type", nullable = false, length = 32)
    private String betType;

    @Column(name = "bet_option")
    private Short betOption;

    @Column(name = "odds", nullable = false, precision = 12, scale = 4)
    private BigDecimal odds;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
