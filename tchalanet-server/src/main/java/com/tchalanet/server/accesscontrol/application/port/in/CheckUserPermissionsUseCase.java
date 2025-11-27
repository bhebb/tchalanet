package com.tchalanet.server.accesscontrol.application.port.in;

import com.tchalanet.server.accesscontrol.domain.exception.PermissionsDeniedException;
import java.util.Collection;
import java.util.UUID;

/** Use case principal d'enforcement d'autorisations. (Port d'entrée) */
public interface CheckUserPermissionsUseCase {

  /**
   * Vérifie que l'utilisateur possède toutes les permissions demandées.
   *
   * <p>Sémantique : - si toutes les permissions sont accordées -> ne fait rien (retour normal) -
   * sinon -> lève PermissionsDeniedException
   */
  void check(UUID tenantId, UUID userId, Collection<String> permissionCodes)
      throws PermissionsDeniedException;
}
