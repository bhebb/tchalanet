package com.tchalanet.server.features.tenantadmin.readiness.model;

/**
 * Coarse-grained readiness status for a tenant or a readiness section.
 */
public enum TenantReadinessStatus {
  READY,
  PARTIAL,
  MISSING,
  UNKNOWN
}
