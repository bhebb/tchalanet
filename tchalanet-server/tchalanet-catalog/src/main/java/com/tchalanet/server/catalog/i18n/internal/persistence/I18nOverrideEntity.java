package com.tchalanet.server.catalog.i18n.internal.persistence;

import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import com.tchalanet.server.common.persistence.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "i18n_override")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class I18nOverrideEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private I18nOverrideLevel level = I18nOverrideLevel.TENANT;

    /**
     * NULL for GLOBAL, non-null for TENANT.
     * Enforced by DB CHECK constraint ck_i18n_override_target.
     */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "i18n_key", nullable = false, length = 255)
    private String i18nKey;

    @Column(name = "i18n_value", nullable = false, columnDefinition = "TEXT")
    private String i18nValue;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public String fullKey() {
        return locale + ":" + i18nKey;
    }
}
