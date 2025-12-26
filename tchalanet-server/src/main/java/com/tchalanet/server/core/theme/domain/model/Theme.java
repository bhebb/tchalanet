package com.tchalanet.server.core.theme.domain.model;

import com.tchalanet.server.common.types.enums.ThemeMode;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Theme(
    UUID id,
    TenantId tenantId,
    String basePresetId,
    String label,
    ThemeMode mode,
    short density,
    Map<String, Object> palette,
    Map<String, Object> tokens,
    Map<String, String> cssVars,
    ThemeStatus status,
    int themeVersion,
    Instant createdAt,
    Instant updatedAt) {

  public Theme {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(tenantId, "tenantId is required");
    requireNonBlank(basePresetId, "basePresetId is required");
    requireNonBlank(label, "label is required");
    Objects.requireNonNull(mode, "mode is required");
    Objects.requireNonNull(status, "status is required");

    if (density < 0 || density > 2) {
      throw new IllegalArgumentException("density must be between 0 and 2");
    }

    palette = palette == null ? Map.of() : Collections.unmodifiableMap(palette);
    tokens = tokens == null ? Map.of() : Collections.unmodifiableMap(tokens);
    cssVars = cssVars == null ? Map.of() : Collections.unmodifiableMap(cssVars);
  }

  public static Theme draft(
      UUID id,
      TenantId tenantId,
      String basePresetId,
      String label,
      ThemeMode mode,
      short density,
      Map<String, Object> palette,
      Map<String, Object> tokens,
      Map<String, String> cssVars,
      Instant now) {
    return new Theme(
        id,
        tenantId,
        basePresetId,
        label,
        mode,
        density,
        palette,
        tokens,
        cssVars,
        ThemeStatus.DRAFT,
        0,
        now,
        now);
  }

  public Theme publish(Instant now) {
    if (status == ThemeStatus.ARCHIVED) {
      throw new IllegalStateException("Cannot publish an archived theme");
    }
    if (status == ThemeStatus.PUBLISHED) {
      return this; // idempotent
    }
    if (status != ThemeStatus.DRAFT) {
      throw new IllegalStateException("Theme must be DRAFT to be published");
    }
    return withStatus(ThemeStatus.PUBLISHED, now);
  }

  public Theme archive(Instant now) {
    if (status == ThemeStatus.ARCHIVED) {
      return this; // idempotent
    }
    return withStatus(ThemeStatus.ARCHIVED, now);
  }

  public Theme updateDraft(
      String label,
      ThemeMode mode,
      short density,
      Map<String, Object> palette,
      Map<String, Object> tokens,
      Map<String, String> cssVars,
      Instant now) {
    if (status != ThemeStatus.DRAFT) {
      throw new IllegalStateException("Only DRAFT themes can be edited");
    }
    return new Theme(
        id,
        tenantId,
        basePresetId,
        label != null ? label : this.label,
        mode != null ? mode : this.mode,
        density,
        palette != null ? palette : this.palette,
        tokens != null ? tokens : this.tokens,
        cssVars != null ? cssVars : this.cssVars,
        status,
        themeVersion,
        createdAt,
        now);
  }

  private Theme withStatus(ThemeStatus newStatus, Instant now) {
    return new Theme(
        id,
        tenantId,
        basePresetId,
        label,
        mode,
        density,
        palette,
        tokens,
        cssVars,
        newStatus,
        themeVersion,
        createdAt,
        now);
  }

  private static void requireNonBlank(String v, String msg) {
    if (v == null || v.isBlank()) {
      throw new IllegalArgumentException(msg);
    }
  }
}
