package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Ouvre les tirages dont l'heure programmée est imminente.
 *
 * @param now          horodatage de référence (UTC) pour le calcul de la fenêtre
 * @param limit        nombre maximum de tirages à ouvrir par exécution (garde-fou anti-boucle)
 * @param openHorizonHours nombre d'heures à l'avance avant {@code scheduledAt} à partir duquel
 *                         un tirage devient éligible à l'ouverture
 * @param openLagHours délai de grâce en heures après {@code cutoffAt} :
 *                     un tirage reste ouvert jusqu'à {@code cutoffAt + openLagHours}
 * @param dryRun       si {@code true}, simule sans aucune écriture en base
 */
public record OpenDueDrawsCommand(
    @NotNull Instant now,
    @Positive int limit,
    @Positive int openHorizonHours,
    @Positive int openLagHours,
    boolean dryRun)
    implements Command<OpenDueDrawsResult> {}
