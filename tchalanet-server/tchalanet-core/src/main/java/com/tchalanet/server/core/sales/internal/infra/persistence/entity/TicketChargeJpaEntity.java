package com.tchalanet.server.core.sales.internal.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.money.TicketChargeType;
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
import org.hibernate.envers.Audited;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "sales_ticket_charge",
    indexes = {
        @Index(name = "idx_sales_ticket_charge_tenant", columnList = "tenant_id"),
        @Index(name = "idx_sales_ticket_charge_ticket", columnList = "sales_ticket_id"),
        @Index(name = "idx_sales_ticket_charge_type", columnList = "tenant_id, charge_type"),
        @Index(name = "idx_sales_ticket_charge_paid_by", columnList = "tenant_id, paid_by")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_sales_ticket_charge_type",
            columnNames = {"tenant_id", "sales_ticket_id", "charge_type", "paid_by"}
        )
    }
)
public class TicketChargeJpaEntity extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "sales_ticket_id",
        nullable = false,
        updatable = false,
        foreignKey = @ForeignKey(name = "fk_sales_ticket_charge_ticket")
    )
    private TicketJpaEntity ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 48, updatable = false)
    private TicketChargeType chargeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_by", nullable = false, length = 16, updatable = false)
    private ChargePaidBy paidBy;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "waived_by_rule_id", updatable = false)
    private java.util.UUID waivedByRuleId;
}
