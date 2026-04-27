package com.tchalanet.server.common.security;

/**
 * API scopes used to classify endpoints.
 *
 * <p>- PUBLIC: public endpoints + swagger + actuator - PLATFORM: platform catalog / system
 * endpoints (no tenant) - TENANT: tenant-scoped endpoints (tenant required: /tenant/** and
 * /admin/**)
 */
public enum ApiScope {
  PUBLIC,
  PLATFORM,
  TENANT
}
