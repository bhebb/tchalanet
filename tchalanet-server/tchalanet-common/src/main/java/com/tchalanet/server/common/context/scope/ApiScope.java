package com.tchalanet.server.common.context.scope;

/**
 * API scopes used to classify endpoints.
 *
 * <p>- PUBLIC: public endpoints + swagger + actuator - PLATFORM: platform catalog / system
 * endpoints (no tenant) - SDR: Spring Data REST internals - ADMIN: tenant administration
 * endpoints - TENANT: tenant business endpoints.
 */
public enum ApiScope {
  PUBLIC,
  PLATFORM,
  ADMIN,
  TENANT
}
