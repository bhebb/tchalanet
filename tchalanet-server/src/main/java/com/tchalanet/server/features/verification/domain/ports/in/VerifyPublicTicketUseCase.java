package com.tchalanet.server.features.verification.domain.ports.in;

import com.tchalanet.server.features.verification.domain.model.PublicTicketStatus;

/**
 * Use case de vérification publique d'un ticket via son code public. Pourra être exposé via un
 * contrôleur public type /api/public/tickets/{publicCode}.
 */
public interface VerifyPublicTicketUseCase {

  /**
   * Vérifie l'état public d'un ticket.
   *
   * @param publicCode code public fourni par le joueur
   * @param lang langue demandée (pour les messages éventuels)
   * @return un statut public (VALID, CANCELED, PENDING, EXPIRED, UNKNOWN)
   */
  PublicTicketStatus verify(String publicCode, String lang);
}
