package com.tchalanet.server.common.context;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Helper to run code with a temporary TchRequestContext for tenant-scoped startup/batch code.
 *
 * @deprecated Use {@link TchContextScope}; callers should choose explicit startup or temporary
 *     tenant semantics.
 */
@Deprecated(forRemoval = false)
public final class TchContextRunner {
  private TchContextRunner() {}

  public static void runAsTenant(UUID tenantId, String requestId, Runnable r) {
    TchContextScope.runWithTemporaryTenant(tenantId, requestId, r);
  }

  public static <T> T runAsTenantResult(UUID tenantId, String requestId, Supplier<T> s) {
    return TchContextScope.runWithTemporaryTenantResult(tenantId, requestId, s);
  }
}
