package com.tchalanet.server.platform.tenantgame.internal.cache;

/** Cache names for tenant game runtime/projection reads (evicted on any tenant game write). */
public final class TenantGameCacheNames {

  private TenantGameCacheNames() {}

  /** Enabled tenant games for POS/runtime, keyed by tenant. */
  public static final String RUNTIME = "platform:tenantgame:runtime";

  /** Full catalog projection (enabled + available) for admin/config, keyed by tenant. */
  public static final String PROJECTION = "platform:tenantgame:projection";
}
