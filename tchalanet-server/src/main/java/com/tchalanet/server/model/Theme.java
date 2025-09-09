package com.tchalanet.server.model;

import com.tchalanet.server.constants.ThemeMode;
import com.tchalanet.server.constants.ThemeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "theme")
@Getter
@Setter
@NoArgsConstructor
public class Theme {
  @Id
  private UUID id;

  @Column(name = "tenant_id")
  private UUID tenantId;

  @Column(name = "base_preset_id", nullable = false)
  private String basePresetId;

  @Column(nullable = false)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ThemeMode mode = ThemeMode.SYSTEM;

  @Column(nullable = false)
  private short density = 0;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "palette_json", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> palette = new HashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "tokens_json", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> tokens = new HashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "css_vars_json", columnDefinition = "jsonb", nullable = false)
  private Map<String, String> cssVars = new HashMap<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ThemeStatus status = ThemeStatus.DRAFT;

  @Version
  @Column(nullable = false)
  private Integer version = 1;

  @Column(name = "created_at", updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @PrePersist
  void prePersist() {
    createdAt = updatedAt = OffsetDateTime.now();
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
