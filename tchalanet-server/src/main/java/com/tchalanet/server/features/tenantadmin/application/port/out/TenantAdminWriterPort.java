package com.tchalanet.server.features.tenantadmin.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public interface TenantAdminWriterPort {
  void suspendMembership(TenantId tenantId, UserId userId, String reason);

  void reactivateMembership(TenantId tenantId, UserId userId);

  void assignRole(TenantId tenantId, UserId userId, com.tchalanet.server.common.types.id.RoleId roleId);

  void changeAutonomy(TenantId tenantId, UserId userId, com.tchalanet.server.common.types.enums.AutonomyLevel autonomyLevel);
}
