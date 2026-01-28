package com.tchalanet.server.features.tenantadmin.infra.persistence;

import com.tchalanet.server.features.tenantadmin.application.port.out.TenantAdminWriterPort;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import org.springframework.stereotype.Component;

@Component
public class TenantAdminWriterAdapter implements TenantAdminWriterPort {

  @Override
  public void suspendMembership(TenantId tenantId, UserId userId, String reason) {
    // TODO: delegate to core.tenantuser writer port or command bus
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void reactivateMembership(TenantId tenantId, UserId userId) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void assignRole(TenantId tenantId, UserId userId, RoleId roleId) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void changeAutonomy(TenantId tenantId, UserId userId, AutonomyLevel autonomyLevel) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
