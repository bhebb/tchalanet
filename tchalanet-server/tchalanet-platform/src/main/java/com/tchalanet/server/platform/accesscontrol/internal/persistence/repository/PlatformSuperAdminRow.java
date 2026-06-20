package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import java.time.Instant;
import java.util.UUID;

public interface PlatformSuperAdminRow {
  UUID getUserId();
  String getEmail();
  String getDisplayName();
  String getStatus();
  Instant getAssignedAt();
}
