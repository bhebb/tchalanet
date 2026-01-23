package com.tchalanet.server.catalog.theme.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;
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

    @Lob
    @Column(name = "config", columnDefinition = "text", nullable = false)
    private String config; // store JSON as text; mapper converts to JsonNode

    @Column(name = "label_key", length = 255)
    private String labelKey;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "is_default", nullable = false)
    private boolean defaultPreset = false;
}
