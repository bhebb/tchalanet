package com.tchalanet.server.platform.tenant.internal.domain;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.TenantType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

/**
 * Aggregate: TenantConfig - manages tenant registry and lifecycle. Per DOMAIN_TENANT_CONFIG.md: -
 * Owns tenant creation, lifecycle transitions, field updates - Publishes events after-commit for
 * other domains to react - No RLS on tenant table (platform registry)
 */
public record TenantConfig(
    TenantId id,
    String code, // immutable, normalized lower-case, unique
    String name,
    String displayName,
    TenantType type,
    ZoneId timezone,
    Currency currency,
    TenantStatus status, // DRAFT|ACTIVE|SUSPENDED|REJECTED|ARCHIVED
    JsonNode config,
    AddressId addressId, // optional
    ThemePresetId activeThemeId, // optional
    BigDecimal defaultCommissionRate // optional
    ) {

  public TenantConfig {
    Objects.requireNonNull(id, "id is required");
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code is required and non-blank");
    }
    if (code.length() > 64) {
      throw new IllegalArgumentException("code must be <= 64 chars");
    }
    if (!code.equals(code.trim().toLowerCase())) {
      throw new IllegalArgumentException("code must be normalized (trim + lowercase)");
    }
    Objects.requireNonNull(name, "name is required");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name is non-blank");
    }
    Objects.requireNonNull(displayName, "displayName is required");
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName is non-blank");
    }
    Objects.requireNonNull(type, "type is required");
    Objects.requireNonNull(timezone, "timezone is required");
    Objects.requireNonNull(currency, "currency is required");
    Objects.requireNonNull(status, "status is required");
  }

  /** Factory: create a new tenant in DRAFT status. */
  public static TenantConfig createDraft(
      TenantId id,
      String code,
      String name,
      TenantType type,
      ZoneId timezone,
      Currency currency,
      AddressId addressId,
      ThemePresetId activeThemeId,
      BigDecimal defaultCommissionRate,
      JsonNode config) {
    var normalizedCode = normalizeCode(code);
    return new TenantConfig(
        id,
        normalizedCode,
        name,
        normalizedCode,
        type,
        timezone,
        currency,
        TenantStatus.DRAFT,
        config,
        addressId, // no address initially
        activeThemeId,
        defaultCommissionRate);
  }

  /**
   * Factory: restore TenantConfig from TenantRegistryView (catalog). Per user request: eliminates
   * need for separate mapper. Converts catalog view to mutable domain model for mutations.
   */
  public static TenantConfig fromRegistryView(TenantContextLookupView registryView) {
    return new TenantConfig(
        registryView.tenantId(),
        registryView.code(),
        registryView.name(),
        registryView.displayName(),
        registryView.type(),
        registryView.timezone(), // ZoneId → ZoneId (no conversion!)
        registryView.currency(), // Currency → Currency (no conversion!)
        registryView.status(),
        null,
        registryView.addressId().orElse(null), // Optional<AddressId> → AddressId
        registryView.activeThemeId().orElse(null), // Optional<ThemePresetId> → ThemePresetId
        registryView.defaultCommissionRate().orElse(null));
  }

  /** Normalize code: trim + lowercase. */
  private static String normalizeCode(String raw) {
    var normalized = raw.trim().toLowerCase();
    if (normalized.isEmpty() || normalized.length() > 64) {
      throw new IllegalArgumentException("code must be 1-64 chars after normalization");
    }
    return normalized;
  }

  /** Activate tenant (DRAFT → ACTIVE). */
  public TenantConfig activate(Instant now) {
    if (status == TenantStatus.ARCHIVED || status == TenantStatus.REJECTED) {
      throw new IllegalStateException("Cannot activate " + status + " tenant");
    }
    if (status == TenantStatus.ACTIVE) {
      return this; // idempotent
    }
    return new TenantConfig(
        id,
        code,
        name,
        displayName,
        type,
        timezone,
        currency,
        TenantStatus.ACTIVE,
        config,
        addressId,
        activeThemeId,
        defaultCommissionRate);
  }

  /** Suspend tenant (ACTIVE → SUSPENDED). */
  public TenantConfig suspend(Instant now) {
    if (status != TenantStatus.ACTIVE) {
      throw new IllegalStateException("Cannot suspend non-ACTIVE tenant");
    }
    return new TenantConfig(
        id,
        code,
        name,
        displayName,
        type,
        timezone,
        currency,
        TenantStatus.SUSPENDED,
        config,
        addressId,
        activeThemeId,
        defaultCommissionRate);
  }

  /** Archive tenant (any → ARCHIVED). */
  public TenantConfig archive(Instant now) {
    if (status == TenantStatus.ARCHIVED) {
      return this; // idempotent
    }
    return new TenantConfig(
        id,
        code,
        name,
        displayName,
        type,
        timezone,
        currency,
        TenantStatus.ARCHIVED,
        config,
        addressId,
        activeThemeId,
        defaultCommissionRate);
  }

  /** Rename tenant. */
  public TenantConfig rename(String newName, Instant now) {
    Objects.requireNonNull(newName, "newName is required");
    if (newName.isBlank()) {
      throw new IllegalArgumentException("name is non-blank");
    }
    return new TenantConfig(
        id,
        code,
        newName,
        displayName,
        type,
        timezone,
        currency,
        status,
        config,
        addressId,
        activeThemeId,
        defaultCommissionRate);
  }

  public TenantConfig renameDisplay(String newDisplayName, Instant now) {
    Objects.requireNonNull(newDisplayName, "newDisplayName is required");
    if (newDisplayName.isBlank()) {
      throw new IllegalArgumentException("displayName is non-blank");
    }
    return new TenantConfig(
        id,
        code,
        name,
        newDisplayName,
        type,
        timezone,
        currency,
        status,
        config,
        addressId,
        activeThemeId,
        defaultCommissionRate);
  }

  /** Update locale (timezone + currency). */
  public TenantConfig updateLocale(ZoneId newTimezone, Currency newCurrency, Instant now) {
    Objects.requireNonNull(newTimezone, "newTimezone is required");
    Objects.requireNonNull(newCurrency, "newCurrency is required");
    return new TenantConfig(
        id,
        code,
        name,
        displayName,
        type,
        newTimezone,
        newCurrency,
        status,
        config,
        addressId,
        activeThemeId,
        defaultCommissionRate);
  }

  /** Set tenant address. */
  public TenantConfig withAddressId(AddressId newAddressId, Instant now) {
    return new TenantConfig(
        id,
        code,
        name,
        displayName,
        type,
        timezone,
        currency,
        status,
        config,
        newAddressId,
        activeThemeId,
        defaultCommissionRate);
  }

  /** Overload for use when newAddressId is not yet an Instant arg (backward compat). */
  public TenantConfig withAddressId(AddressId newAddressId) {
    return new TenantConfig(
        id,
        code,
        name,
        displayName,
        type,
        timezone,
        currency,
        status,
        config,
        newAddressId,
        activeThemeId,
        defaultCommissionRate);
  }

  /** Clear tenant address. */
  public TenantConfig clearAddressId(Instant now) {
    return new TenantConfig(
        id,
        code,
        name,
        displayName,
        type,
        timezone,
        currency,
        status,
        config,
        null,
        activeThemeId,
        defaultCommissionRate);
  }

  /** Set active theme. */
  public TenantConfig withActiveThemeId(ThemePresetId themeId, Instant now) {
    return new TenantConfig(
        id,
        code,
        name,
        displayName,
        type,
        timezone,
        currency,
        status,
        config,
        addressId,
        themeId,
        defaultCommissionRate);
  }

  /** Clear active theme. */
  public TenantConfig clearActiveThemeId(Instant now) {
    return new TenantConfig(
        id,
        code,
        name,
        displayName,
        type,
        timezone,
        currency,
        status,
        config,
        addressId,
        null,
        defaultCommissionRate);
  }

  public TenantConfig updateConfig(JsonNode newConfig, Instant now) {
    Objects.requireNonNull(newConfig, "newConfig is required");
    return new TenantConfig(
        id,
        code,
        name,
        displayName,
        type,
        timezone,
        currency,
        status,
        newConfig,
        addressId,
        activeThemeId,
        defaultCommissionRate);
  }
}
