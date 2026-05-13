package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
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
@Table(name = "offline_code_reservation")
public class OfflineCodeReservationJpaEntity extends BaseTenantEntity {

  @Column(name = "code_batch_id", nullable = false)
  private UUID codeBatchId;

  @Column(name = "offline_code", nullable = false)
  private String offlineCode;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "reserved_at", nullable = false)
  private Instant reservedAt;

  @Column(name = "consumed_at")
  private Instant consumedAt;

}

