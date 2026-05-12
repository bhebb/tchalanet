package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Entity
@Audited
@Table(name = "offline_code_batch")
public class OfflineCodeBatchJpaEntity extends BaseTenantEntity {

  @Column(name = "terminal_id", nullable = false)
  private UUID terminalId;

  @Column(name = "allocated_count", nullable = false)
  private Integer allocatedCount;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Version
  private Long version;
}

