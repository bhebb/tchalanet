package com.tchalanet.server.platform.tenant.internal.persistence;

import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.TenantType;
import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * JPA entity for TenantConfig.
 * Table: tenant (no RLS, platform registry)
 * Per DOMAIN_TENANT_CONFIG.md:
 * - Platform-wide tenant registry (no RLS needed)
 * - Unique constraint on code
 * - Optional references to address and theme
 * Per typed_ids.md: UUID in entity, typed IDs in domain
 */
@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantJpaEntity extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TenantType type;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "default_language", nullable = false, length = 8)
    private String defaultLanguage;

    @Column(name = "default_locale", nullable = false, length = 16)
    private String defaultLocale;

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TenantStatus status;

    @Column(name = "address_id", columnDefinition = "UUID")
    private UUID addressId;

    @Column(name = "active_theme_id", columnDefinition = "UUID")
    private UUID activeThemeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private String config;
}
