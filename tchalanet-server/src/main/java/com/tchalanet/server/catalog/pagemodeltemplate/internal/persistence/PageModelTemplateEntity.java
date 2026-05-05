package com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence;

import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateLevel;
import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "page_model_template")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class PageModelTemplateEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 128, unique = true)
    private String code;

    @Column(name = "logical_id", nullable = false)
    private String logicalId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "label")
    private String label;

    @Column(name = "description")
    private String description;

    @Column(name = "schema", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String schema;

    @Column(name = "model", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String model;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion = 1;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 16)
    private PageModelTemplateLevel level = PageModelTemplateLevel.GLOBAL;

    @Column(name = "tenant_id")
    private UUID tenantId; // null when GLOBAL
}
