package com.tchalanet.server.common.context;

import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.security.ApiScope;

import java.time.ZoneId;
import java.util.Currency;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Helper to run code with a temporary TchRequestContext for tenant-scoped startup/batch code.
 */
public final class TchContextRunner {
  private TchContextRunner() {}

  public static void runAsTenant(UUID tenantId, String requestId, Runnable r) {
    var ctx = new TchRequestContext(
        "tchalanet",
        tenantId,
        "tchalanet",
        tenantId,
        null,
        null,
        EnumSet.noneOf(TchRole.class),
        Set.of(),
        Locale.getDefault(),
        requestId == null ? "startup" : requestId,
        "127.0.0.1",
        null,
        false,
        "active",
        ApiScope.TENANT,
        null,
        TenantId.nullableOf(tenantId),
        ZoneId.systemDefault(),
        Currency.getInstance(CommonConstants.DEFAULT_CURRENCY)
    );
    TchContext.set(ctx);
    try {
      r.run();
    } finally {
      TchContext.clear();
    }
  }

  public static <T> T runAsTenantResult(UUID tenantId, String requestId, Supplier<T> s) {
    var ctx = new TchRequestContext(
        "tchalanet",
        tenantId,
        "tchalanet",
        tenantId,
        null,
        null,
        EnumSet.noneOf(TchRole.class),
        Set.of(),
        Locale.getDefault(),
        requestId == null ? "startup" : requestId,
        "127.0.0.1",
        null,
        false,
        "active",
        ApiScope.TENANT,
        null,
        TenantId.nullableOf(tenantId),
        ZoneId.systemDefault(),
        Currency.getInstance(CommonConstants.DEFAULT_CURRENCY)
    );
    TchContext.set(ctx);
    try {
      return s.get();
    } finally {
      TchContext.clear();
    }
  }
}
