package com.tchalanet.server.core.accesscontrol.application.port.out;

import java.util.Set;
import java.util.UUID;

/** Port de sortie pour lire les permissions d'un rôle via sa hiérarchie. */
public interface RolePermissionReaderPort {

  /** Retourne tous les codes de permissions accordés à un rôle (incluant la hiérarchie). */
  Set<String> findPermissionCodesForRoleHierarchy(UUID roleId);

  /** Indique si la hiérarchie de rôle contient une permission donnée. */
  boolean roleHierarchyHasPermission(UUID roleId, String permissionCode);
}
