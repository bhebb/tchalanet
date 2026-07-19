package com.tchalanet.server.common.context;

import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.context.tenant.TenantContextInfo;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record TchRequestContext(
    String originalTenantCode,
    UUID originalTenantUuid,
    String effectiveTenantCode,
    UUID effectiveTenantUuid,
    UUID appUserId,
    // @deprecated — use roleCodes() instead
    Set<TchRole> systemRoles,
    // @deprecated — use permissionKeys() instead
    Set<String> customRoles,
    Locale locale,
    String requestId,
    String clientIp,
    String userAgent,
    boolean tenantOverridden,
    String tenantOverrideReason,
    String deletedVisibility,
    ApiScope apiScope,
    String idempotencyKey,
    TenantId tenantId,
    ZoneId tenantZoneId,
    Currency tenantCurrency,
    OperationalContextHint operationalContext,
    // Provider-neutral actor fields (Slice 1 — provider-neutral-access-context-v1)
    TchActorType actorType,
    SellerTerminalId sellerTerminalId,
    Set<String> roleCodes,
    Set<String> permissionKeys,
    String externalSubject) {

  public TchRequestContext {
    systemRoles = systemRoles == null ? Set.of() : Set.copyOf(systemRoles);
    customRoles = customRoles == null ? Set.of() : Set.copyOf(customRoles);
    roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    permissionKeys = permissionKeys == null ? Set.of() : Set.copyOf(permissionKeys);
  }

  /** Return the effective tenant UUID when available, otherwise the original one. */
  public UUID tenantUuid() {
    return effectiveTenantUuid != null ? effectiveTenantUuid : originalTenantUuid;
  }

  public TenantId tenantId() {
    return tenantIdSafe();
  }

  /**
   * Compatibility: return the application user id as string when available. If app user is not
   * known yet, this returns null.
   */
  public UserId userId() {
    return UserId.nullableOf(appUserId);
  }

  /** Return the application user UUID (if present) */
  public UUID userUuid() {
    return appUserId;
  }

  public String userAgent() {
    return userAgent;
  }

  /**
   * Rôle principal courant dérivé de systemRoles, avec priorité : SUPER_ADMIN > TENANT_OWNER >
   * TENANT_ADMIN.
   */
  public TchRole currentRole() {
    if (systemRoles == null || systemRoles.isEmpty()) {
      return null;
    }

    if (systemRoles.contains(TchRole.SUPER_ADMIN)) {
      return TchRole.SUPER_ADMIN;
    }

    if (systemRoles.contains(TchRole.TENANT_OWNER)) {
      return TchRole.TENANT_OWNER;
    }

    if (systemRoles.contains(TchRole.TENANT_ADMIN)) {
      return TchRole.TENANT_ADMIN;
    }

    return null;
  }

  public String deletedVisibilitySafe() {
    String v = (deletedVisibility == null ? "active" : deletedVisibility.trim().toLowerCase());
    return (v.equals("active") || v.equals("deleted") || v.equals("all")) ? v : "active";
  }

  public boolean isSuperAdmin() {
    return systemRoles != null && systemRoles.contains(TchRole.SUPER_ADMIN);
  }

  /** Convenience builder to return a new TchRequestContext with a different deletedVisibility. */
  public TchRequestContext withEffectiveTenantUuid(UUID uuid) {
    return new TchRequestContext(
        originalTenantCode,
        originalTenantUuid,
        effectiveTenantCode,
        uuid,
        appUserId,
        systemRoles,
        customRoles,
        locale,
        requestId,
        clientIp,
        userAgent,
        tenantOverridden,
        tenantOverrideReason,
        deletedVisibility,
        apiScope,
        idempotencyKey,
        tenantId,
        tenantZoneId,
        tenantCurrency,
        operationalContext,
        actorType,
        sellerTerminalId,
        roleCodes,
        permissionKeys,
        externalSubject);
  }

  public TchRequestContext withAppUserId(UUID userId) {
    return new TchRequestContext(
        originalTenantCode,
        originalTenantUuid,
        effectiveTenantCode,
        effectiveTenantUuid,
        userId,
        systemRoles,
        customRoles,
        locale,
        requestId,
        clientIp,
        userAgent,
        tenantOverridden,
        tenantOverrideReason,
        deletedVisibility,
        apiScope,
        idempotencyKey,
        tenantId,
        tenantZoneId,
        tenantCurrency,
        operationalContext,
        actorType,
        sellerTerminalId,
        roleCodes,
        permissionKeys,
        externalSubject);
  }

  /**
   * @deprecated Use {@link #withResolvedAccess} for new provider-neutral code.
   */
  @Deprecated
  public TchRequestContext withAuthorization(
      Set<TchRole> resolvedSystemRoles, Set<String> resolvedPermissions) {
    return new TchRequestContext(
        originalTenantCode,
        originalTenantUuid,
        effectiveTenantCode,
        effectiveTenantUuid,
        appUserId,
        resolvedSystemRoles == null ? Set.of() : Set.copyOf(resolvedSystemRoles),
        resolvedPermissions == null ? Set.of() : Set.copyOf(resolvedPermissions),
        locale,
        requestId,
        clientIp,
        userAgent,
        tenantOverridden,
        tenantOverrideReason,
        deletedVisibility,
        apiScope,
        idempotencyKey,
        tenantId,
        tenantZoneId,
        tenantCurrency,
        operationalContext,
        actorType,
        sellerTerminalId,
        roleCodes,
        permissionKeys,
        externalSubject);
  }

  public TchRequestContext withResolvedAccess(
      TchActorType resolvedActorType,
      SellerTerminalId resolvedSellerTerminalId,
      Set<String> resolvedRoleCodes,
      Set<String> resolvedPermissionKeys) {
    return new TchRequestContext(
        originalTenantCode,
        originalTenantUuid,
        effectiveTenantCode,
        effectiveTenantUuid,
        appUserId,
        systemRoles,
        customRoles,
        locale,
        requestId,
        clientIp,
        userAgent,
        tenantOverridden,
        tenantOverrideReason,
        deletedVisibility,
        apiScope,
        idempotencyKey,
        tenantId,
        tenantZoneId,
        tenantCurrency,
        operationalContext,
        resolvedActorType,
        resolvedSellerTerminalId,
        resolvedRoleCodes == null ? Set.of() : Set.copyOf(resolvedRoleCodes),
        resolvedPermissionKeys == null ? Set.of() : Set.copyOf(resolvedPermissionKeys),
        externalSubject);
  }

  /**
   * Return a new context with the given sellerTerminalId injected. Used by the
   * admin-to-seller-terminal bridge (X-Tch-Act-As-Terminal header).
   */
  public TchRequestContext withSellerTerminalId(SellerTerminalId terminal) {
    return new TchRequestContext(
        originalTenantCode,
        originalTenantUuid,
        effectiveTenantCode,
        effectiveTenantUuid,
        appUserId,
        systemRoles,
        customRoles,
        locale,
        requestId,
        clientIp,
        userAgent,
        tenantOverridden,
        tenantOverrideReason,
        deletedVisibility,
        apiScope,
        idempotencyKey,
        tenantId,
        tenantZoneId,
        tenantCurrency,
        operationalContext,
        actorType,
        terminal,
        roleCodes,
        permissionKeys,
        externalSubject);
  }

  public TchRequestContext withIdempotencyKey(String key) {
    return new TchRequestContext(
        originalTenantCode,
        originalTenantUuid,
        effectiveTenantCode,
        effectiveTenantUuid,
        appUserId,
        systemRoles,
        customRoles,
        locale,
        requestId,
        clientIp,
        userAgent,
        tenantOverridden,
        tenantOverrideReason,
        deletedVisibility,
        apiScope,
        key,
        tenantId,
        tenantZoneId,
        tenantCurrency,
        operationalContext,
        actorType,
        sellerTerminalId,
        roleCodes,
        permissionKeys,
        externalSubject);
  }

  public TenantId tenantIdSafe() {
    // priorité au champ typé, sinon fallback sur UUID existants
    if (tenantId != null) return tenantId;
    return TenantId.nullableOf(tenantUuid());
  }

  public TenantId effectiveTenantIdOrNull() {
    return tenantIdSafe();
  }

  public TenantId effectiveTenantIdRequired() {
    TenantId effectiveTenantId = effectiveTenantIdOrNull();
    if (effectiveTenantId == null) {
      throw ProblemRest.of(RequestContextErrorCodes.TENANT_REQUIRED);
    }
    return effectiveTenantId;
  }

  /**
   * Preferred alias for tenant-scoped endpoints. This returns the effective tenant, including any
   * validated support/admin override carried by the request context.
   */
  public TenantId tenantIdRequired() {
    return effectiveTenantIdRequired();
  }

  public boolean hasTenant() {
    return effectiveTenantIdOrNull() != null;
  }

  public boolean isPlatformScope() {
    return apiScope == ApiScope.PLATFORM;
  }

  /**
   * Retourne le UserId applicatif courant ou lève une exception 422 si l'utilisateur n'a pas encore
   * effectué /api/me/bootstrap (appUserId absent).
   */
  public UserId currentUserIdRequired() {
    if (appUserId == null) throw ProblemRest.of(RequestContextErrorCodes.USER_NOT_BOOTSTRAPPED);
    return UserId.of(appUserId);
  }

  public SellerTerminalId sellerTerminalIdRequired() {
    if (sellerTerminalId == null) {
      throw ProblemRest.of(RequestContextErrorCodes.SELLER_TERMINAL_REQUIRED);
    }
    return sellerTerminalId;
  }

  public TchRequestContext withTenantContext(TenantContextInfo info) {
    return new TchRequestContext(
        originalTenantCode,
        originalTenantUuid,
        effectiveTenantCode,
        info.tenantId().value(), // keep existing UUID field in sync
        appUserId,
        systemRoles,
        customRoles,
        locale,
        requestId,
        clientIp,
        userAgent,
        tenantOverridden,
        tenantOverrideReason,
        deletedVisibility,
        apiScope,
        idempotencyKey,
        info.tenantId(),
        info.tenantZoneId(),
        info.currency(),
        operationalContext,
        actorType,
        sellerTerminalId,
        roleCodes,
        permissionKeys,
        externalSubject);
  }

  /** Factory for a minimal startup/batch tenant context. Use for non-HTTP threads. */
  public static TchRequestContext startupTenant(UUID tenantUuid, String requestId) {
    return new TchRequestContext(
        "tchalanet",
        tenantUuid,
        "tchalanet",
        tenantUuid,
        null,
        java.util.EnumSet.noneOf(TchRole.class),
        Set.of(),
        Locale.getDefault(),
        requestId == null ? "startup" : requestId,
        "127.0.0.1",
        null,
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        TenantId.nullableOf(tenantUuid),
        ZoneId.systemDefault(),
        Currency.getInstance(CommonConstants.DEFAULT_CURRENCY),
        null,
        TchActorType.SYSTEM,
        null,
        Set.of(),
        Set.of(),
        null);
  }

  public ApiScope apiScope() {
    return apiScope;
  }

  public boolean isOperator() {
    return false;
  }

  public boolean isTenantAdmin() {
    return systemRoles != null && systemRoles.contains(TchRole.TENANT_ADMIN);
  }

  public boolean isOperationalRole() {
    return isOperator();
  }

  public boolean hasPermissionClaim(String permission) {
    if (permission == null || customRoles == null) {
      return false;
    }

    var normalized = permission.trim().toUpperCase(Locale.ROOT);
    for (var role : customRoles) {
      if (role != null && role.trim().toUpperCase(Locale.ROOT).equals(normalized)) {
        return true;
      }
    }
    return false;
  }

  public OperationalContextHint operationalContextRequired() {
    if (operationalContext == null) {
      throw ProblemRest.of(RequestContextErrorCodes.OPERATIONAL_CONTEXT_REQUIRED);
    }
    return operationalContext;
  }

  public OperationalContextHint trustedOperationalContextRequired() {
    var context = operationalContextRequired();
    if (!context.trustedForSensitiveOperation()) {
      throw ProblemRest.of(RequestContextErrorCodes.OPERATIONAL_CONTEXT_UNTRUSTED);
    }
    return context;
  }

  public TchRequestContext withOperationalContext(OperationalContextHint operationalContext) {
    return new TchRequestContext(
        originalTenantCode,
        originalTenantUuid,
        effectiveTenantCode,
        effectiveTenantUuid,
        appUserId,
        systemRoles,
        customRoles,
        locale,
        requestId,
        clientIp,
        userAgent,
        tenantOverridden,
        tenantOverrideReason,
        deletedVisibility,
        apiScope,
        idempotencyKey,
        tenantId,
        tenantZoneId,
        tenantCurrency,
        operationalContext,
        actorType,
        sellerTerminalId,
        roleCodes,
        permissionKeys,
        externalSubject);
  }

  public CorrelationId correlationId() {
    return requestId == null || requestId.isBlank()
        ? CorrelationId.newId()
        : CorrelationId.of(requestId);
  }
}
