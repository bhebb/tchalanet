package com.tchalanet.server.core.featureflags.domain.model;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing the context for evaluating a feature flag. Contains attributes like
 * tenantId, userId, terminalId, etc., that can influence flag evaluation.
 */
public record FeatureContext(
    UUID tenantId,
    UUID userId,
    UUID terminalId,
    Map<String, String> customProperties // For any other dynamic context
    ) {
  public FeatureContext {
    Objects.requireNonNull(tenantId, "TenantId cannot be null in FeatureContext");
    if (customProperties == null) {
      customProperties = Map.of();
    }
  }

  public static FeatureContext empty(UUID tenantId) {
    return new FeatureContext(tenantId, null, null, Map.of());
  }

  public FeatureContext withUserId(UUID userId) {
    return new FeatureContext(this.tenantId, userId, this.terminalId, this.customProperties);
  }

  public FeatureContext withTerminalId(UUID terminalId) {
    return new FeatureContext(this.tenantId, this.userId, terminalId, this.customProperties);
  }

  public FeatureContext withProperty(String key, String value) {
    Map<String, String> newProps = new java.util.HashMap<>(this.customProperties);
    newProps.put(key, value);
    return new FeatureContext(this.tenantId, this.userId, this.terminalId, newProps);
  }
}
