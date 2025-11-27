package com.tchalanet.server.accesscontrol.application.port.in;

import java.util.List;
import java.util.UUID;

/** Use case admin pour gérer les rôles applicatifs (globaux + spécifiques tenant). */
public interface RoleAdminUseCase {

  record RoleSummary(
      UUID id,
      String code,
      String name,
      String description,
      UUID tenantId,
      UUID parentRoleId,
      boolean system) {}

  /** Création ou mise à jour d'un rôle. - roleId null => création - roleId non null => update */
  UUID upsertRole(
      UUID roleId,
      String code,
      String name,
      String description,
      UUID tenantId,
      UUID parentRoleId,
      boolean system);

  /**
   * Liste des rôles visibles pour un tenant. - tenantId null => roles globaux uniquement - tenantId
   * non null => rôles globaux + rôles du tenant
   */
  List<RoleSummary> listRoles(UUID tenantId);
}
