package com.tchalanet.server.core.promotion.api.command.applied;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.promotion.api.model.AppliedPromotionSnapshotResult;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import jakarta.validation.constraints.NotNull;

/**
 * Consumed from sales after ticket persistence or by promotionDecision event listener.
 * Uses promotionDecision.api.model.PromotionDecision, not sales event payload.
 */
public record CreateAppliedPromotionSnapshotCommand(
    @NotNull TenantId tenantId,
    @NotNull TicketId ticketId,
    @NotNull PromotionDecision decision
) implements Command<AppliedPromotionSnapshotResult> {
}

