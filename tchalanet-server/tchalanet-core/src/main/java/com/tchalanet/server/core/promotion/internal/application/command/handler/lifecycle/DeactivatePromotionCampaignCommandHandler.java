package com.tchalanet.server.core.promotion.internal.application.command.handler.lifecycle;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.promotion.api.command.lifecycle.DeactivatePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCampaignWritePort;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCacheEvictorPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeactivatePromotionCampaignCommandHandler implements CommandHandler<DeactivatePromotionCampaignCommand, PromotionCampaignView> {

    private final PromotionCampaignWritePort writePort;
    private final PromotionCacheEvictorPort cacheEvictor;

    @Override
    public PromotionCampaignView handle(DeactivatePromotionCampaignCommand cmd) {
        var out = writePort.changeStatus(cmd.tenantId(), cmd.campaignId(), com.tchalanet.server.core.promotion.api.model.PromotionCampaignStatus.INACTIVE);
        AfterCommit.run(() -> cacheEvictor.evictAfterCampaignMutation(cmd.tenantId(), cmd.campaignId()));
        return out;
    }
}


