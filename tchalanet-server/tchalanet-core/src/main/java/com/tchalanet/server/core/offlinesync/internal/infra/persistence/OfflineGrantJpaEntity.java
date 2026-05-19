package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Getter
@Setter
@Entity
@Audited
@Table(name = "offline_grant")
public class OfflineGrantJpaEntity extends BaseTenantEntity {

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "outlet_id", nullable = false)
    private UUID outletId;

    @Column(name = "seller_user_id", nullable = false)
    private UUID sellerUserId;

    @Column(name = "sales_session_id", nullable = false)
    private UUID salesSessionId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @NotAudited
    @Column(name = "device_public_key", nullable = false, columnDefinition = "text")
    private String devicePublicKey;

    @Column(name = "key_id", nullable = false)
    private String keyId;

    @Column(name = "code_batch_id")
    private UUID codeBatchId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "sync_accepted_until", nullable = false)
    private Instant syncAcceptedUntil;

    @Column(name = "max_ticket_count")
    private Integer maxTicketCount;

    @Column(name = "max_total_amount", precision = 18, scale = 2)
    private BigDecimal maxTotalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "consumed_ticket_count", nullable = false)
    private Integer consumedTicketCount = 0;

    @Column(name = "consumed_total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal consumedTotalAmount = BigDecimal.ZERO;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason")
    private String revokedReason;
}
