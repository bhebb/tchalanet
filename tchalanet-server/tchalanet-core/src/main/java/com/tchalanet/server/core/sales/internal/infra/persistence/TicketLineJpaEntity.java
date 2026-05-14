package com.tchalanet.server.core.sales.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.sales.api.model.TicketResultStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ticket_line")
@Getter
@Setter
@Audited
public class TicketLineJpaEntity extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketJpaEntity ticket;

    @Column(name = "game_code", nullable = false, length = 32)
    private String gameCode;

    @Column(name = "selection", nullable = false, length = 32)
    private String selection;

    @Column(name = "stake", nullable = false, precision = 12, scale = 2)
    private BigDecimal stake;

    @Column(name = "odds_snapshot", nullable = false, precision = 12, scale = 4)
    private BigDecimal oddsSnapshot;

    @Column(name = "potential_payout", nullable = false, precision = 14, scale = 2)
    private BigDecimal potentialPayout;

    @Column(name = "bet_option")
    private Short betOption;

    @Enumerated(EnumType.STRING)
    @Column(name = "bet_type", nullable = false, length = 32)
    private BetType betType;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "result_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TicketResultStatus resultStatus;

    @Column(name = "winning_amount")
    private BigDecimal winningAmount;
}
