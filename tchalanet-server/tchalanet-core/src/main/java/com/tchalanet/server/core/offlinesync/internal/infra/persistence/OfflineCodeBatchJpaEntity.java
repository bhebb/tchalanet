package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    @Column(name = "grant_id", nullable = false)
    private UUID grantId;

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "outlet_id")
    private UUID outletId;

    @Column(name = "seller_user_id")
    private UUID sellerUserId;

    @Column(name = "allocated_count", nullable = false)
    private Integer allocatedCount;

    @Column(name = "consumed_count", nullable = false)
    private Integer consumedCount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
