package com.tchalanet.server.core.accesscontrol.application.port.out;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;

/** Port de sortie pour lire les rôles (read-only). */
public interface RoleReaderPort {

  /**
   * Retourne les rôles système (TchRole) appartenant au tenant + roles globaux si tenantId non
   * null; si tenantId==null on retourne les rôles globaux.
   */
  List<TchRole> listSystemRolesForTenant(TenantId tenantId);
}
