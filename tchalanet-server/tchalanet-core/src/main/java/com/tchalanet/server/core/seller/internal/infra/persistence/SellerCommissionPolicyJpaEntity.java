package com.tchalanet.server.core.seller.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_commission_policy")
@Audited
@Getter
@Setter
public class SellerCommissionPolicyJpaEntity extends BaseTenantEntity {

    @Column(name = "seller_id", nullable = false, columnDefinition = "uuid")
    private UUID sellerId;

    @Column(name = "type", nullable = false, length = 48)
    private String type;

    @Column(name = "base", nullable = false, length = 48)
    private String base;

    @Column(name = "rate_percent", precision = 9, scale = 4)
    private BigDecimal ratePercent;

    @Column(name = "fixed_amount", precision = 19, scale = 4)
    private BigDecimal fixedAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;
}
