package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Ferme les tirages dont le cutoff est dépassé.
 *
 * @param now    horodatage de référence (UTC) pour déterminer les tirages éligibles
 * @param limit  nombre maximum de tirages à fermer par exécution (garde-fou anti-boucle)
 * @param dryRun si {@code true}, simule sans aucune écriture en base
 */
public record CloseDueDrawsCommand(@NotNull Instant now, int limit, boolean dryRun)
    implements Command<CloseDueDrawsResult> {}
