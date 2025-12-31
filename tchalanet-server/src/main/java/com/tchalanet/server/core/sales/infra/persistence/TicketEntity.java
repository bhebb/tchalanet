package com.tchalanet.server.core.sales.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.types.enums.TicketStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "ticket")
@Getter
@Audited
@Setter
public class TicketEntity extends BaseTenantEntity {

  @Column(name = "terminal_id", nullable = false)
  private UUID terminalId;

  @Column(name = "draw_id", nullable = false)
  private UUID drawId;

  @Column(name = "session_id")
  private UUID sessionId;

  @Column(name = "ticket_code", nullable = false, unique = true)
  private String ticketCode;

  @Column(name = "public_code", length = 32)
  private String publicCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private TicketStatus status;

  @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalAmount;

  @OneToMany(
      mappedBy = "ticket",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<TicketLineEntity> lines = new ArrayList<>();

  public void addLine(TicketLineEntity line) {
    lines.add(line);
    line.setTicket(this);
  }

  public void clearAndAddLines(List<TicketLineEntity> newLines) {
    lines.clear();
    for (var l : newLines) addLine(l);
  }
}
