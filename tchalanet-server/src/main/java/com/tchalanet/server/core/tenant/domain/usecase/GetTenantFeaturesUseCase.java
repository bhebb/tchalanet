package com.tchalanet.server.core.tenant.domain.usecase;

import com.tchalanet.server.core.tenantconfig.domain.model.TenantFeaturesDto;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Use case pour récupérer les fonctionnalités disponibles pour un tenant et un rôle. Version mock -
 * à implémenter avec la vraie logique métier.
 */
@Service
public class GetTenantFeaturesUseCase {

  public TenantFeaturesDto execute(String tenantId, String role) {
    // TODO: Implémenter la vraie logique de récupération des features depuis la configuration
    // Pour le moment, retourne des features mockées
    return new TenantFeaturesDto(
        tenantId, role, List.of("ticket.create", "draw.view", "sales.read"));
  }
}
