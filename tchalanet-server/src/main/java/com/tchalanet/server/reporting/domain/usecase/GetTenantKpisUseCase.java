package com.tchalanet.server.reporting.domain.usecase;

import com.tchalanet.server.reporting.web.dto.KpisDto;
import org.springframework.stereotype.Service;

/**
 * Use case pour récupérer les KPIs (indicateurs clés) d'un tenant. Version mock - à implémenter
 * avec la vraie logique de calcul des KPIs.
 */
@Service
public class GetTenantKpisUseCase {

  public KpisDto execute(String tenantId, String role) {
    // TODO: Implémenter le vrai calcul des KPIs depuis la base de données
    // - Chiffre d'affaires total
    // - Nombre de tickets vendus
    // - Nombre de tirages
    // - Gain moyen

    // Pour le moment, retourne des KPIs mockés
    return new KpisDto(
        1245.50, // totalRevenue
        382, // totalTickets
        4, // totalDraws
        225.75 // averageWin
        );
  }
}
