package com.tchalanet.server.core.promotion.api.command.lifecycle;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.PromotionStackingPolicy;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreatePromotionCampaignCommand(
    @NotNull TenantId tenantId,
    @NotNull String name,
    String description,
    @NotNull Instant startsAt,
    Instant endsAt,
    @NotNull Integer priority,
    @NotNull PromotionStackingPolicy stackingPolicy
) implements Command<PromotionCampaignView> {}


