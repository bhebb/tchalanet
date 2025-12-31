package com.tchalanet.server.core.pos.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "terminal")
@Getter
@Setter
@Audited
public class TerminalJpaEntity extends BaseTenantEntity {

  @Column(name = "outlet_id", nullable = false)
  private UUID outletId;

  @Column(name = "state", nullable = false)
  private String state;

  @Column(name = "last_seen")
  private Instant lastSeen;

  @Column(name = "meta", columnDefinition = "text")
  private String meta; // or JsonNode, but for simplicity String

  @Column(name = "registered_at")
  private Instant registeredAt;

  @Column(name = "unregistered_at")
  private Instant unregisteredAt;

  @Column(name = "locked_at")
  private Instant lockedAt;

  @Column(name = "locked_by")
  private UUID lockedBy;

  @Column(name = "lock_reason")
  private String lockReason;
}
