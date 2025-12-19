package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.core.tenant.domain.model.TenantStatus;
import com.tchalanet.server.core.tenant.domain.model.TenantType;
import com.tchalanet.server.core.tenant.infra.cache.TenantCacheEvictListener;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "tenant")
@EntityListeners({TenantCacheEvictListener.class})
@Audited
@Getter
@Setter
@NoArgsConstructor
public class TenantJpaEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 64, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TenantStatus status; // DRAFT|ACTIVE|SUSPENDED|REJECTED|ARCHIVED

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private TenantType type;

    @Column(name = "active_theme_id")
    private UUID activeThemeId;

    @Column(name = "address_id")
    private UUID addressId;
}
