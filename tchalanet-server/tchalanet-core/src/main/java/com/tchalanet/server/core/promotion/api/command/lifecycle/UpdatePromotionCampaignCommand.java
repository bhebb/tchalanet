package com.tchalanet.server.core.promotion.api.command.lifecycle;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.PromotionCampaignId;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record UpdatePromotionCampaignCommand(
	@NotNull TenantId tenantId,
	@NotNull PromotionCampaignId campaignId,
	@NotNull String name,
	String description,
	@NotNull Instant startsAt,
	Instant endsAt,
	@NotNull Integer priority
) implements Command<PromotionCampaignView> {}

