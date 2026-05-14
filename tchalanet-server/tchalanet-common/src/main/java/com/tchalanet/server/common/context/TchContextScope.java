package com.tchalanet.server.common.context;

import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Explicit scope helpers for non-HTTP context binding and deliberate temporary tenant switches.
 *
 * <p>Normal HTTP request handling should use the context built by {@code TchContextFilter}.
 */
public final class TchContextScope {

  private TchContextScope() {}

  public static void runStartupTenant(UUID tenantId, String requestId, Runnable work) {
    runWithContext(startupTenantContext(tenantId, requestId), work);
  }

  public static <T> T runStartupTenantResult(UUID tenantId, String requestId, Supplier<T> work) {
    return runWithContextResult(startupTenantContext(tenantId, requestId), work);
  }

  public static void runWithTemporaryTenant(UUID tenantId, String requestId, Runnable work) {
    runWithContext(temporaryTenantContext(tenantId, requestId), work);
  }

  public static <T> T runWithTemporaryTenantResult(UUID tenantId, String requestId, Supplier<T> work) {
    return runWithContextResult(temporaryTenantContext(tenantId, requestId), work);
  }

  public static void runWithContext(TchRequestContext ctx, Runnable work) {
    var previous = TchContext.currentOrNull();
    TchContext.set(ctx);
    try {
      work.run();
    } finally {
      restore(previous);
    }
  }

  public static <T> T runWithContextResult(TchRequestContext ctx, Supplier<T> work) {
    var previous = TchContext.currentOrNull();
    TchContext.set(ctx);
    try {
      return work.get();
    } finally {
      restore(previous);
    }
  }

  private static TchRequestContext startupTenantContext(UUID tenantId, String requestId) {
    return tenantContext(tenantId, requestId == null ? "startup" : requestId);
  }

  private static TchRequestContext temporaryTenantContext(UUID tenantId, String requestId) {
    return tenantContext(tenantId, requestId == null ? "temporary-tenant" : requestId);
  }

  private static TchRequestContext tenantContext(UUID tenantId, String requestId) {
    return new TchRequestContext(
        "tchalanet",
        tenantId,
        "tchalanet",
        tenantId,
        null,
        null,
        EnumSet.noneOf(TchRole.class),
        Set.of(),
        Locale.getDefault(),
        requestId,
        "127.0.0.1",
        null,
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        TenantId.nullableOf(tenantId),
        ZoneId.systemDefault(),
        Currency.getInstance(CommonConstants.DEFAULT_CURRENCY),
        null);
  }

  private static void restore(TchRequestContext previous) {
    if (previous == null) {
      TchContext.clear();
      return;
    }
    TchContext.set(previous);
  }
}
