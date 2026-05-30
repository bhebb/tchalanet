package com.tchalanet.server.catalog.tenant.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-model JPA entity for tenant registry.
 * Mapped to tenant table (or dedicated tenant_registry table later).
 * Per DOMAIN_TENANT_CATALOG.md: read-only, bypasses RLS via raw EntityManager.
 */
@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantRegistryJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name", length = 256)
    private String name;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // ACTIVE, SUSPENDED, ARCHIVED

    @Column(name = "type", nullable = false, length = 32)
    private String type;   // COMMERCIAL, PERSONAL

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "default_language", length = 8)
    private String defaultLanguage;

    @Column(name = "default_locale", length = 16)
    private String defaultLocale;

    @Column(name = "address_id")
    private UUID addressId;

    @Column(name = "active_theme_id", columnDefinition = "UUID")
    private UUID activeThemeId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
