package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Ouvre les tirages dont l'heure programmée est imminente.
 *
 * @param now          horodatage de référence (UTC) pour le calcul de la fenêtre
 * @param batchSize    nombre maximum de tirages à ouvrir par exécution (garde-fou anti-boucle)
 * @param lookaheadHours nombre d'heures à l'avance avant {@code scheduledAt} à partir duquel
 *                         un tirage devient éligible à l'ouverture
 * @param lagHours delay de grâce en heures après {@code cutoffAt} :
 *                     un tirage reste ouvert jusqu'à {@code cutoffAt + lagHours}
 * @param dryRun       si {@code true}, simule sans aucune écriture en base
 */
public record OpenDueDrawsCommand(
    @NotNull Instant now,
    @Positive int batchSize,
    @Positive int lookaheadHours,
    @Positive int lagHours,
    boolean dryRun)
    implements Command<OpenDueDrawsResult> {}
