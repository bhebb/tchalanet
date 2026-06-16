package com.tchalanet.server.core.seller.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.UUID;

@Entity
@Table(
    name = "seller",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_seller_tenant_code_active", columnNames = {"tenant_id", "code"})
    }
)
@Getter
@Setter
public class SellerJpaEntity extends BaseTenantEntity {

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "code", length = 64)
    private String code;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "status", nullable = false, length = 32)
    private String status;
}
