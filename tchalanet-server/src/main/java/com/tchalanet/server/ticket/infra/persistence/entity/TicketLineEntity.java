package com.tchalanet.server.ticket.infra.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ticket_line")
@Getter
@Setter
public class TicketLineEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false)
  private TicketEntity ticket;

  @Column(name = "game_code", nullable = false)
  private String gameCode;

  @Column(nullable = false)
  private String selection;

  @Column(nullable = false)
  private BigDecimal stake;

  @Column(name = "odds_snapshot", nullable = false)
  private BigDecimal oddsSnapshot;

  @Column(name = "potential_payout", nullable = false)
  private BigDecimal potentialPayout;
}
