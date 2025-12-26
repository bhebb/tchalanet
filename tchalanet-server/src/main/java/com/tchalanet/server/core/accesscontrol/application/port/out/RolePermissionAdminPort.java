package com.tchalanet.server.core.accesscontrol.application.port.out;

import com.tchalanet.server.common.types.id.RoleId;

import java.util.Set;
import java.util.UUID;

/** Port de sortie pour la gestion des liens rôle -> permissions (opérations atomiques). */
public interface RolePermissionAdminPort {

  /**
   * Accorde une permission à un rôle.
   * @return true si un nouveau lien a été créé, false si le lien existait déjà (idempotent).
   */
  boolean grant(RoleId roleId, String permissionCode);

  /**
   * Révoque une permission d'un rôle.
   * @return true si un lien a été supprimé, false si aucun lien n'existait.
   */
  boolean revoke(RoleId roleId, String permissionCode);

  /**
   * Liste les codes de permissions associés à un rôle.
   */
  Set<String> listPermissionCodes(RoleId roleId);
}
