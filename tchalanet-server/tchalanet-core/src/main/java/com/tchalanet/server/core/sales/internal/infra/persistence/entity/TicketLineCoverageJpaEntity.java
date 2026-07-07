package com.tchalanet.server.core.sales.internal.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.WinMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "sales_ticket_line_coverage",
    indexes = {
        @Index(name = "idx_ticket_line_coverage_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ticket_line_coverage_line", columnList = "ticket_line_id"),
        @Index(
            name = "idx_ticket_line_coverage_variant",
            columnList = "tenant_id, pricing_variant_code"
        )
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_ticket_line_coverage_variant",
            columnNames = {"tenant_id", "ticket_line_id", "pricing_variant_code"}
        )
    }
)
public class TicketLineCoverageJpaEntity extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "ticket_line_id",
        nullable = false,
        updatable = false,
        foreignKey = @ForeignKey(name = "fk_ticket_line_coverage__line")
    )
    private TicketLineJpaEntity ticketLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_variant_code", nullable = false, length = 64, updatable = false)
    private PricingVariantCode pricingVariantCode;

    @Column(name = "stake_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal stakeAmount;

    @Column(name = "odds_snapshot", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal oddsSnapshot;

    @Column(
        name = "potential_gain_snapshot",
        nullable = false,
        precision = 19,
        scale = 4,
        updatable = false
    )
    private BigDecimal potentialGainSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "win_mode", nullable = false, length = 16, updatable = false)
    private WinMode winMode;
}
