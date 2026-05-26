package com.tchalanet.server.core.seller.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_outlet_assignment")
@Audited
@Getter
@Setter
public class SellerOutletAssignmentJpaEntity extends BaseTenantEntity {

    @Column(name = "seller_id", nullable = false, columnDefinition = "uuid")
    private UUID sellerId;

    @Column(name = "outlet_id", nullable = false, columnDefinition = "uuid")
    private UUID outletId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;
}
