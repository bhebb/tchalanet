package com.tchalanet.server.core.sales.internal.infra.persistence.entity;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.util.UUID;


/**
 * JPA entity for {@link com.tchalanet.server.core.sales.domain.model.ticket.TicketLine}.
 *
 * <p>Belongs to a {@link TicketJpaEntity}; cascaded via the parent.
 * Currency is inherited from the parent ticket (not stored on the line).
 */
@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "sales_ticket_line",
    indexes = {
        @Index(name = "idx_ticket_line_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ticket_line_ticket", columnList = "ticket_id"),
        @Index(name = "idx_ticket_line_tenant_draw_game",
            columnList = "tenant_id, draw_id, game_code"),
        @Index(name = "idx_ticket_line_result_status",
            columnList = "tenant_id, result_status")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_ticket_line_number",
            columnNames = {"tenant_id", "ticket_id", "line_number"}
        )
    }
)
public class TicketLineJpaEntity extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "ticket_id",
        nullable = false,
        updatable = false,
        foreignKey = @ForeignKey(name = "fk_ticket_line_ticket")
    )
    private TicketJpaEntity ticket;

    @Column(name = "draw_id", nullable = false, columnDefinition = "uuid", updatable = false)
    private UUID drawId;

    @Column(name = "line_number", nullable = false, updatable = false)
    private int lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_code", nullable = false, length = 64, updatable = false)
    private GameCode gameCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "bet_type", nullable = false, length = 64, updatable = false)
    private BetType betType;

    @Column(name = "bet_option", nullable = false, updatable = false)
    public Short betOption;

    @Column(name = "selection_key", nullable = false, length = 128, updatable = false)
    private String selectionKey;

    @Column(name = "display_selection", nullable = false, length = 256, updatable = false)
    private String displaySelection;

    @Column(name = "stake_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal stakeAmount;

    @Column(name = "odds_snapshot", precision = 19, scale = 6, updatable = false)
    private BigDecimal oddsSnapshot;

    @Column(name = "potential_payout_amount", nullable = false,
        precision = 19, scale = 4, updatable = false)
    private BigDecimal potentialPayoutAmount;

    @Column(name = "payout_base_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal payoutBaseAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 16)
    private TicketLineOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_source", nullable = false, length = 16)
    private TicketLinePricingSource pricingSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_source", nullable = false, length = 32)
    private TicketLineSelectionSource selectionSource;

    @Column(name = "promotion_decision_id", columnDefinition = "uuid")
    private UUID promotionDecisionId;

    @Column(name = "promotion_label", length = 128)
    private String promotionLabel;

    @Column(name = "promotion_effect_type", length = 32)
    private String promotionEffectType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 16)
    private TicketLineResultStatus resultStatus;

    @Column(name = "payout_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal payoutAmount;

}
