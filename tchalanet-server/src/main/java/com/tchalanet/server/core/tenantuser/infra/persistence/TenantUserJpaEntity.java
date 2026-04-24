package com.tchalanet.server.core.tenantuser.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.TenantUserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.UUID;

@Entity
@Table(name = "tenant_user", uniqueConstraints = @UniqueConstraint(name = "ux_tenant_user_tenant_user", columnNames = {"tenant_id", "user_id"}))
@Audited
@Getter
@Setter
public class TenantUserJpaEntity extends BaseTenantEntity {

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(name = "role_id", nullable = true)
    private UUID roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private TenantUserStatus status;

    @Column(name = "is_owner")
    private Boolean isOwner = Boolean.FALSE;

    // NEW: workplace fields
    @Column(name = "outlet_id")
    private UUID outletId;

    @Column(name = "terminal_id")
    private UUID terminalId;
}
