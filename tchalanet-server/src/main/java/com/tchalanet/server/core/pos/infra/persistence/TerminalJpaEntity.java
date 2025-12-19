package com.tchalanet.server.core.pos.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "terminal")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class TerminalJpaEntity extends BaseTenantEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "outlet_id", nullable = false)
  private OutletJpaEntity outlet;

  @Column(name = "state", nullable = false)
  private String state;

  @Column(name = "last_seen")
  private Instant lastSeen;

  // Ticket relation omitted until ticket slice is implemented
}
