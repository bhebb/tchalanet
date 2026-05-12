package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.List;
import java.util.Optional;

public interface TenantUserDirectoryPort {

  /** Retourne la membership active (rôle unique) pour un user dans un tenant, ou vide si aucune. */
  Optional<TenantUserSnapshot> findActiveMembership(TenantId tenantId, UserId userId);

  List<RoleId> getUserRolesInTenant(UserId userId, TenantId tenantId);
}
