package com.tchalanet.server.core.tenant.domain.model;

import com.tchalanet.server.common.types.enums.TenantStatus;
import com.tchalanet.server.common.types.enums.TenantType;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Objects;
import java.util.UUID;

public final class Tenant {

  private final TenantId id;
  private final String code; // immutable
  private String name;
  private TenantType type;
  private String timezone;
  private String currency;
  private TenantStatus status;

  private UUID activeThemeId; // optional
  private UUID addressId; // optional
  private long version;

  private Tenant(
      TenantId id,
      String code,
      String name,
      TenantType type,
      String timezone,
      String currency,
      TenantStatus status,
      UUID activeThemeId,
      UUID addressId,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.code = normalizeCode(code);
    this.name = requireNonBlank(name, "name");
    this.type = Objects.requireNonNull(type, "type");
    this.timezone = requireNonBlank(timezone, "timezone");
    this.currency = requireNonBlank(currency, "currency");
    this.status = Objects.requireNonNull(status, "status");
    this.activeThemeId = activeThemeId;
    this.addressId = addressId;
    this.version = version;
  }

  public static Tenant createDraft(
      TenantId id, String code, String name, TenantType type, String timezone, String currency) {
    return new Tenant(id, code, name, type, timezone, currency, TenantStatus.DRAFT, null, null, 0L);
  }

  /** Rehydrate from persistence */
  public static Tenant restore(
      TenantId id,
      String code,
      String name,
      TenantType type,
      String timezone,
      String currency,
      TenantStatus status,
      UUID activeThemeId,
      UUID addressId,
      long version) {
    return new Tenant(
        id, code, name, type, timezone, currency, status, activeThemeId, addressId, version);
  }

  // --- State machine ---
  public void activate() {
    if (status == TenantStatus.ARCHIVED) throw new IllegalStateException("Tenant is ARCHIVED");
    if (status == TenantStatus.REJECTED) throw new IllegalStateException("Tenant is REJECTED");
    this.status = TenantStatus.ACTIVE;
  }

  public void suspend() {
    if (status != TenantStatus.ACTIVE) {
      throw new IllegalStateException("Only ACTIVE tenant can be suspended. Current=" + status);
    }
    this.status = TenantStatus.SUSPENDED;
  }

  public void archive() {
    if (status == TenantStatus.ARCHIVED) return;
    this.status = TenantStatus.ARCHIVED;
  }

  public void rename(String newName) {
    this.name = requireNonBlank(newName, "name");
  }

  // --- Getters ---
  public TenantId id() {
    return id;
  }

  public String code() {
    return code;
  }

  public String name() {
    return name;
  }

  public TenantType type() {
    return type;
  }

  public String timezone() {
    return timezone;
  }

  public String currency() {
    return currency;
  }

  public TenantStatus status() {
    return status;
  }

  public UUID activeThemeId() {
    return activeThemeId;
  }

  public UUID addressId() {
    return addressId;
  }

  public long version() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  private static String normalizeCode(String code) {
    String v = requireNonBlank(code, "code").trim().toLowerCase();
    if (v.length() > 64) throw new IllegalArgumentException("code too long");
    return v;
  }

  private static String requireNonBlank(String s, String field) {
    if (s == null || s.isBlank()) throw new IllegalArgumentException(field + " is required");
    return s;
  }
}
