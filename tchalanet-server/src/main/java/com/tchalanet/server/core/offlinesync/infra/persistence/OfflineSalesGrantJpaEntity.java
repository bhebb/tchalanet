package com.tchalanet.server.core.offlinesync.infra.persistence;

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
@Table(name = "offline_sales_grant")
public class OfflineSalesGrantJpaEntity extends BaseTenantEntity {

  @Column(name = "terminal_id", nullable = false)
  private UUID terminalId;

  @Column(name = "outlet_id", nullable = false)
  private UUID outletId;

  @Column(name = "seller_user_id", nullable = false)
  private UUID sellerUserId;

  @Column(name = "sales_session_id", nullable = false)
  private UUID salesSessionId;

  @Column(name = "code_batch_id", nullable = false)
  private UUID codeBatchId;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Version
  private Long version;
}

