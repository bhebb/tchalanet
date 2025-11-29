package com.tchalanet.server.core.accesscontrol.application.port.in;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Use case admin pour consulter les permissions et gérer le mapping rôle -> permissions. */
public interface PermissionAdminUseCase {

  record PermissionSummary(String code, String name, String category, String description) {}

  /** Liste toutes les permissions disponibles dans le système. */
  List<PermissionSummary> listPermissions();

  /** Retourne la liste des permissions (codes) associées à un rôle (sans héritage). */
  Set<String> getRolePermissions(UUID roleId);

  /** Remplace l'ensemble des permissions associées à un rôle (sans toucher aux rôles parents). */
  void setRolePermissions(UUID roleId, Set<String> permissionCodes);
}
