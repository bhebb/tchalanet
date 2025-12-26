package com.tchalanet.server.core.accesscontrol.infra.persistence;

import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "app_role")
@Getter
@Setter
@NoArgsConstructor
@Audited
public class AppRoleEntity extends BaseTenantEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "parent_role_id")
    private UUID parentRoleId;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    // Inverse side of the role -> permission mapping (role_permission)
    @OneToMany(
        mappedBy = "role",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private List<AppRolePermissionEntity> rolePermissions = new ArrayList<>();

    // Explicit accessors to help static analysis tools that may not resolve Lombok-generated methods
    public UUID getId() {
        return super.getId();
    }

    public void setId(UUID id) {
        super.setId(id);
    }

    public UUID getTenantId() {
        return super.getTenantId();
    }

    public void setTenantId(UUID tenantId) {
        super.setTenantId(tenantId);
    }
}
