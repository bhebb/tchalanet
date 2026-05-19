package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.offline;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Entity
@Audited
@Table(name = "tenant_offline_policy")
public class TenantOfflinePolicyJpaEntity extends BaseTenantEntity {

    @Column(name = "offline_enabled", nullable = false)
    private Boolean offlineEnabled;

    @Column(name = "batch_size", nullable = false)
    private Integer batchSize;

    @Column(name = "validity_duration_iso", nullable = false, length = 64)
    private String validityDurationIso;

    @Column(name = "sync_accepted_extension_iso", nullable = false, length = 64)
    private String syncAcceptedExtensionIso;

    @Column(name = "max_ticket_count", nullable = false)
    private Integer maxTicketCount;

    @Column(name = "max_total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal maxTotalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
}
