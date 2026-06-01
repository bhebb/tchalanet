package com.tchalanet.server.catalog.theme.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "theme_preset")
@Getter
@Setter
@Audited
public class ThemePresetJpaEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 128, unique = true)
    private String code;

    @Column(name = "vendor", length = 128)
    private String vendor;

    // NOTE: no @Lob — on PostgreSQL @Lob on a String maps the column as a large-object
    // OID (read via getLong), which makes Hibernate fail with "Bad value for type long"
    // when reading this text/JSON column. Plain text mapping is correct here.
    @Column(name = "config", columnDefinition = "text", nullable = false)
    private String config; // store JSON as text; mapper converts to JsonNode

    @Column(name = "label_key", length = 255)
    private String labelKey;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "is_default", nullable = false)
    private boolean defaultPreset = false;
}
