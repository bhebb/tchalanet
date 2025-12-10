package com.tchalanet.server.features.pagemodel.shared;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Nouvelle entité PageModel alignée sur le design 2025.
 */
@Entity
@Table(name = "page_model")
@Getter
@Setter
@Audited
public class PageModelEntity extends BaseTenantEntity {

    @Column(name = "logical_id", nullable = false)
    private String logicalId;

    @Column(name = "scope", nullable = false)
    private String scope;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "model", columnDefinition = "jsonb", nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PageStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "template_id")
    private UUID templateId;
}
