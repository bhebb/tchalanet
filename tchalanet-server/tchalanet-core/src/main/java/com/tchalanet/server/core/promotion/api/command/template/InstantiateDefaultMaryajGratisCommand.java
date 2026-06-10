package com.tchalanet.server.core.promotion.api.command.template;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import jakarta.validation.constraints.NotNull;

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
    @NotNull TenantId tenantId
) implements Command<PromotionCampaignView> {}
