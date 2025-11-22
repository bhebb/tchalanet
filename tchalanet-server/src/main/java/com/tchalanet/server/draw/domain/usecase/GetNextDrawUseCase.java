package com.tchalanet.server.draw.domain.usecase;

import com.tchalanet.server.reporting.web.dto.DrawSummaryDto;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Use case pour récupérer le prochain tirage d'un tenant. Version mock - à implémenter avec la
 * vraie logique de récupération depuis la base de données.
 */
@Service
public class GetNextDrawUseCase {

  public DrawSummaryDto execute(String tenantId) {
    // TODO: Implémenter la vraie logique de récupération du prochain tirage
    // - Requête vers la table des tirages
    // - Filtrer par tenant et status OPEN
    // - Trier par date de tirage
    // - Retourner le premier

    // Pour le moment, retourne un tirage mocké
    return new DrawSummaryDto(
        "d1", // drawId
        "Midi", // drawName
        Instant.now().plusSeconds(3600), // drawTime (dans 1h)
        "OPEN" // status
        );
  }
}
