package com.tchalanet.server.core.draw.api.command;

import com.tchalanet.server.common.bus.Command;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;

/**
 * Opens scheduled draws in a bounded upcoming horizon.
 *
 * @param now horodatage de référence (UTC) pour le calcul de la fenêtre
 * @param batchSize nombre maximum de tirages à ouvrir par exécution (garde-fou anti-boucle)
 * @param lookaheadHours nombre d'heures à l'avance avant {@code scheduledAt} à partir duquel un
 *     tirage devient éligible à l'ouverture
 * @param lagHours bounded recovery window in hours before {@code now}; expired draws are never
 *     selected because {@code cutoffAt > now} remains required
 * @param dryRun si {@code true}, simule sans aucune écriture en base
 */
public record OpenDueDrawsCommand(
    @NotNull Instant now,
    @Positive int batchSize,
    @Positive int lookaheadHours,
    @PositiveOrZero int lagHours,
    boolean dryRun)
    implements Command<OpenDueDrawsResult> {}
