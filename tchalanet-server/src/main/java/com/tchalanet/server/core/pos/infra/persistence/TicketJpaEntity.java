package com.tchalanet.server.core.pos.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "ticket")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class TicketJpaEntity extends BaseTenantEntity {

  @Column(name = "public_code", length = 32)
  private String publicCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "terminal_id", nullable = false)
  private TerminalJpaEntity terminal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "draw_id")
  private DrawJpaEntity draw;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalAmount;

  @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY)
  private List<TicketLineJpaEntity> lines = new ArrayList<>();
}
