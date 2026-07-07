package com.tchalanet.server.core.pricing.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pricing_odds")
@Getter
@Setter
public class TenantPricingOddsJpaEntity extends BaseTenantEntity {

    @Column(name = "game_code", nullable = false, length = 64)
    private String gameCode;

    @Column(name = "pricing_variant_code", nullable = false, length = 64)
    private String pricingVariantCode;

    @Column(name = "bet_type", nullable = false, length = 32)
    private String betType;

    @Column(name = "bet_option")
    private Short betOption;

    @Column(name = "odds", nullable = false, precision = 12, scale = 4)
    private BigDecimal odds;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
