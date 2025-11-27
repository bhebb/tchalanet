package com.tchalanet.server.tenantconfig.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root for a Tenant-specific configuration setting. Represents a key-value pair for a
 * tenant.
 */
public class TenantSetting {

  private final UUID id;
  private final UUID tenantId;
  private String configKey;
  private String configValue;
  private String configType; // e.g., "STRING", "BOOLEAN", "INTEGER", "JSON"
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;

  private TenantSetting(
      UUID id,
      UUID tenantId,
      String configKey,
      String configValue,
      String configType,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.configKey = configKey;
    this.configValue = configValue;
    this.configType = configType;
    this.active = active;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static TenantSetting create(
      UUID tenantId, String configKey, String configValue, String configType) {
    Objects.requireNonNull(tenantId, "TenantId cannot be null");
    Objects.requireNonNull(configKey, "ConfigKey cannot be null");
    Objects.requireNonNull(configValue, "ConfigValue cannot be null");
    Objects.requireNonNull(configType, "ConfigType cannot be null");
    return new TenantSetting(
        UUID.randomUUID(),
        tenantId,
        configKey,
        configValue,
        configType,
        true,
        Instant.now(),
        Instant.now());
  }

  public static TenantSetting load(
      UUID id,
      UUID tenantId,
      String configKey,
      String configValue,
      String configType,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    return new TenantSetting(
        id, tenantId, configKey, configValue, configType, active, createdAt, updatedAt);
  }

  // --- Business Methods ---
  public void update(String configValue, String configType, boolean active) {
    Objects.requireNonNull(configValue, "ConfigValue cannot be null");
    Objects.requireNonNull(configType, "ConfigType cannot be null");
    this.configValue = configValue;
    this.configType = configType;
    this.active = active;
    this.updatedAt = Instant.now();
  }

  // --- Getters ---
  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getConfigKey() {
    return configKey;
  }

  public String getConfigValue() {
    return configValue;
  }

  public String getConfigType() {
    return configType;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
