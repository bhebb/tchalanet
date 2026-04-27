package com.tchalanet.server.core.pos.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

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

  @Column(name = "label", length = 128)
  private String label;

  @Column(name = "inventory_tag", length = 64)
  private String inventoryTag;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
  private JsonNode metadataJson; // stored as jsonb in DB

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
