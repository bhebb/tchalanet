package com.tchalanet.server.core.drawresult.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawResultId;

import java.time.Instant;

/**
 * Commande pour marquer un DrawResult comme OVERRIDDEN.
 * Utilisée suite à une correction de résultat appliqué à un draw.
 */
public record MarkDrawResultOverriddenCommand(
    DrawResultId drawResultId,
    String reason,
    Instant overriddenAt
) implements Command<Void> {
}

