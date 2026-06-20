package com.tchalanet.server.common.context.scope;

/**
 * API scopes used to classify endpoints.
 *
 * <p>- PUBLIC: public endpoints + swagger + actuator - PLATFORM: platform catalog / system
 * endpoints (no tenant) - IDENTITY: authenticated identity/runtime bootstrap endpoints with no
 * tenant requirement - ADMIN: tenant administration endpoints - TENANT: tenant business endpoints.
 */
public enum ApiScope {
  PUBLIC,
  PLATFORM,
  IDENTITY,
  ADMIN,
  TENANT
}
