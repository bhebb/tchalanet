package com.tchalanet.server.platform.tenanttheme.internal.cache;

/** Cache names for tenant theme runtime reads (evicted on any tenant theme write). */
public final class TenantThemeCacheNames {

  private TenantThemeCacheNames() {}

  /** Resolved ThemeRuntimeView, keyed by tenant + requested mode. */
  public static final String RUNTIME = "platform:tenanttheme:runtime";
}
