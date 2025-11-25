package com.tchalanet.server.common.usecase;

import com.tchalanet.server.common.web.dto.PublicHomeData;
import java.util.UUID;

/**
 * Use case d'orchestration pour la page publique: agrège les données nécessaires (plans, jeux,
 * tirages, prochain tirage) pour un tenant donné.
 */
public interface GetPublicHomePageUseCase {

  /**
   * Récupère les données dynamiques pour la page publique du tenant.
   *
   * @param tenantId tenant cible (peut être null pour un mode "global" plateforme)
   */
  PublicHomeData get(UUID tenantId);
}
