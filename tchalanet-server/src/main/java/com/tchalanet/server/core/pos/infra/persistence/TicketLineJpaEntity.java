package com.tchalanet.server.core.pos.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "ticket_line")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class TicketLineJpaEntity {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false)
  private TicketJpaEntity ticket;

  @Column(name = "game_code", nullable = false)
  private String gameCode;

  @Column(name = "selection", nullable = false)
  private String selection;

  @Column(name = "stake", nullable = false, precision = 12, scale = 2)
  private BigDecimal stake;

  @Column(name = "odds_snapshot", nullable = false, precision = 12, scale = 4)
  private BigDecimal oddsSnapshot;

  @Column(name = "potential_payout", nullable = false, precision = 14, scale = 2)
  private BigDecimal potentialPayout;
}
