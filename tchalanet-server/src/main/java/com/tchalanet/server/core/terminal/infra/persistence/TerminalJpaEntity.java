package com.tchalanet.server.core.terminal.infra.persistence;

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

  @Column(name = "assigned_user_id")
  private UUID assignedUserId;

  @Column(name = "kind", nullable = false, length = 16)
  private String kind = "PHYSICAL";

  @Column(name = "state", nullable = false, length = 32)
  private String state;

  @Column(name = "active_for_user", nullable = false)
  private boolean activeForUser = false;

  @Column(name = "sync_state", nullable = false, length = 32)
  private String syncState = "ONLINE";

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
