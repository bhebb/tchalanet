package com.tchalanet.server.catalog.pricing.internal.persistence;

import com.tchalanet.server.common.types.enums.BetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import com.tchalanet.server.common.persistence.BaseTenantEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_odds")
@Getter
@Setter
@Audited
public class PricingOddsEntity extends BaseTenantEntity {

    @Column(name = "game_code", nullable = false, length = 32)
    private String gameCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "bet_type", nullable = false, length = 32)
    private BetType betType;

    @Column(name = "bet_option")
    private Short betOption;

    @Column(name = "odds", nullable = false, precision = 12, scale = 4)
    private BigDecimal odds;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}

