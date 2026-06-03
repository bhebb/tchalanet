package com.tchalanet.server.platform.tenanttheme.internal.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "tenant_theme")
@Getter
@Setter
public class TenantThemeJpaEntity extends BaseTenantEntity {

    @Column(name = "preset_code", nullable = false, length = 128)
    private String presetCode;

    @Column(name = "default_mode", nullable = false, length = 16)
    private String defaultMode = "SYSTEM";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "is_default", nullable = false)
    private boolean defaultTheme = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "token_overrides", columnDefinition = "jsonb")
    private Map<String, String> tokenOverrides;
}
