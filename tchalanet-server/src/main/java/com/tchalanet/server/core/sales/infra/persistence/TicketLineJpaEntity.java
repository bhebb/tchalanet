package com.tchalanet.server.core.sales.infra.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Entity
@Audited
@Table(name = "ticket_line")
public class TicketLineJpaEntity {

  @Id
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "ticket_id", nullable = false)
  private UUID ticketId;

  @Column(name = "line_no", nullable = false)
  private Integer lineNo;

  @Column(name = "game_code", nullable = false)
  private String gameCode;

  @Column(name = "selection", nullable = false)
  private String selection;

  @Column(name = "bet_type", nullable = false)
  private String betType;

  @Column(name = "bet_option")
  private Short betOption;

  @Column(name = "stake_amount", nullable = false)
  private BigDecimal stakeAmount;

  @Column(name = "odds_snapshot", nullable = false)
  private BigDecimal oddsSnapshot;

  @Column(name = "potential_payout_amount", nullable = false)
  private BigDecimal potentialPayoutAmount;

  @Column(name = "result_status", nullable = false)
  private String resultStatus;

  @Column(name = "winning_amount")
  private BigDecimal winningAmount;

  @Version
  private Long version;
}
