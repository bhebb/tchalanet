package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.List;

/**
 * Applique les résultats externes disponibles aux tirages d'un tenant sur une fenêtre glissante.
 *
 * @param tenantId  tenant ciblé
 * @param baseDate  date de référence (point le plus récent de la fenêtre)
 * @param daysBack  nombre de jours en arrière à inclure dans la fenêtre (0 = baseDate seulement)
 * @param slotKeys  liste des clés de slots à traiter (ex : {@code US_NY_NUM3_MID})
 * @param force     si {@code true}, réapplique même si le draw est déjà à l'état {@code RESULTED}
 * @param dryRun    si {@code true}, simule sans aucune écriture en base
 * @param maxSlots  nombre maximum de slots traités par appel (garde-fou anti-boucle)
 */
public record ApplyExternalResultsWindowCommand(
    TenantId tenantId,
    LocalDate baseDate,
    int daysBack,
    List<String> slotKeys,
    boolean force,
    boolean dryRun,
    int maxSlots)
    implements Command<ApplyExternalResultsWindowResult> {}
