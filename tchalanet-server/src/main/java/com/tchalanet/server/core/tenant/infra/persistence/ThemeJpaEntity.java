package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.persistence.MapStringToJsonConverter;
import com.tchalanet.server.common.persistence.MapToJsonConverter;
import com.tchalanet.server.core.tenant.domain.model.ThemeMode;
import com.tchalanet.server.core.tenant.domain.model.ThemeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "theme")
@Getter
@Setter
@NoArgsConstructor
public class ThemeJpaEntity extends BaseTenantEntity {
  @Column(name = "base_preset_id", nullable = false, length = 128)
  private String basePresetId;

  @Column(name = "label", nullable = false, length = 160)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(name = "mode", nullable = false, length = 10)
  private ThemeMode mode; // light|dark|system

  @Column(name = "density", nullable = false)
  private short density;

  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "palette_json", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> palette;

  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "tokens_json", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> tokens;

  @Convert(converter = MapStringToJsonConverter.class)
  @Column(name = "css_vars_json", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> cssVars;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ThemeStatus status; // draft|published|archived

  @Column(name = "theme_version", nullable = false)
  private int themeVersion;
}
