package com.tchalanet.server.core.promotion.internal.application.command.handler.lifecycle;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.promotion.api.command.lifecycle.ArchivePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCampaignWritePort;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCacheEvictorPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ArchivePromotionCampaignCommandHandler implements CommandHandler<ArchivePromotionCampaignCommand, PromotionCampaignView> {

    private final PromotionCampaignWritePort writePort;
    private final PromotionCacheEvictorPort cacheEvictor;

    @Override
    public PromotionCampaignView handle(ArchivePromotionCampaignCommand cmd) {
        var out = writePort.changeStatus(cmd.tenantId(), cmd.campaignId(), com.tchalanet.server.core.promotion.api.model.PromotionCampaignStatus.ARCHIVED);
        AfterCommit.run(() -> cacheEvictor.evictAfterCampaignMutation(cmd.tenantId(), cmd.campaignId()));
        return out;
    }
}


