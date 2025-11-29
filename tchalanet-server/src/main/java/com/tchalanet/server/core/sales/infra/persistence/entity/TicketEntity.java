package com.tchalanet.server.core.sales.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.sales.domain.model.TicketStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ticket")
@Getter
@Setter
public class TicketEntity extends BaseTenantEntity { // Extends BaseTenantEntity

  // ID is now inherited from BaseEntity
  // @Id
  // private UUID id;

  // tenantId is now inherited from BaseTenantEntity
  // @Column(name = "tenant_id", nullable = false)
  // todo handle
  private UUID sessionId;

  @Column(name = "terminal_id")
  private UUID terminalId;

  @Column(name = "draw_id", nullable = false)
  private UUID drawId;

  @Column(name = "ticket_code", nullable = false, unique = true)
  private String ticketCode;

  @Column(name = "public_code", nullable = false, unique = true)
  private String publicCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TicketStatus status;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @OneToMany(
      mappedBy = "ticket",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<TicketLineEntity> lines = new ArrayList<>();
}
