package com.tchalanet.server.core.outlet.internal.infra.persistence;

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
    name = "sales_zone",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_sales_zone_tenant_code", columnNames = {"tenant_id", "code"})
    }
)
@Audited
@Getter
@Setter
public class SalesZoneJpaEntity extends BaseTenantEntity {

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "label", nullable = false, length = 160)
    private String label;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "parent_id", columnDefinition = "uuid")
    private UUID parentId;
}
