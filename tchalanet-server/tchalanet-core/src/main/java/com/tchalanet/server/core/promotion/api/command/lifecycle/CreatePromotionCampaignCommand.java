package com.tchalanet.server.core.promotion.api.command.lifecycle;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.api.model.rule.PromotionRuleConfigInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record CreatePromotionCampaignCommand(
    @NotNull TenantId tenantId,
    @NotNull String name,
    String description,
    @NotNull Instant startsAt,
    Instant endsAt,
    @NotNull Integer priority,
    @NotEmpty List<@Valid PromotionRuleConfigInput> rules
) implements Command<PromotionCampaignView> {}
