package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import java.time.Instant;
import java.util.UUID;

/** Projection row for the global tenant-admin listing (SUPER_ADMIN only). */
public interface TenantAdminGlobalRow {
  UUID getUserId();
  String getEmail();
  String getDisplayName();
  String getStatus();
  UUID getTenantId();
  String getTenantName();
  String getTenantCode();
  Instant getAssignedAt();
}
