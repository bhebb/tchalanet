package com.tchalanet.server.core.sales.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.common.types.enums.BetType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ticket_line")
@Getter
@Setter
public class TicketLineEntity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ticket_id", nullable = false)
  private TicketEntity ticket;

  @Column(name = "game_code", nullable = false, length = 32)
  private String gameCode;

  @Column(name = "selection", nullable = false)
  private String selection;

  @Column(name = "stake", nullable = false, precision = 12, scale = 2)
  private BigDecimal stake;

  @Column(name = "odds_snapshot", nullable = false, precision = 12, scale = 4)
  private BigDecimal oddsSnapshot;

  @Column(name = "potential_payout", nullable = false, precision = 14, scale = 2)
  private BigDecimal potentialPayout;

  @Enumerated(EnumType.STRING)
  @Column(name = "bet_type", nullable = false)
  private BetType betType;
}
