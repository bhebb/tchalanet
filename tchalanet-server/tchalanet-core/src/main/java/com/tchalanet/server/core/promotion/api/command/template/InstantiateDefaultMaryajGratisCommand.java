package com.tchalanet.server.core.promotion.api.command.template;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.PromotionChoiceMode;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.selection.api.model.SelectionGenerationStrategy;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Instantiates the platform default "Maryaj gratuit" campaign for a tenant
 * (template -> tenant instance, never a shared runtime campaign) and activates
 * it. Idempotent: returns the existing campaign when the code already exists.
 * <p>
 * Source de vérité : {@code openspec/changes/maryaj-gratis-auto-selection-v1/}.
 * The onboarding hook (features.platformadmin) and the existing-tenant
 * backfill are explicit follow-ups; this command is the V1 entry point.
 */
public record InstantiateDefaultMaryajGratisCommand(
    @NotNull TenantId tenantId,
    BigDecimal payoutBaseAmount,
    Integer quantity,
    PromotionChoiceMode choiceMode,
    SelectionGenerationStrategy generationStrategy,
    Boolean regenerableBeforeConfirm,
    Integer maxRegenerationsBeforeConfirm
) implements Command<PromotionCampaignView> {
    public InstantiateDefaultMaryajGratisCommand(TenantId tenantId) {
        this(tenantId, null, null, null, null, null, null);
    }
}
