package com.tchalanet.server.core.promotion.internal.application.command.handler.lifecycle;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.promotion.api.command.lifecycle.PausePromotionCampaignCommand;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCacheEvictorPort;
import com.tchalanet.server.core.promotion.internal.application.port.out.lifecycle.PromotionCampaignReadPort;
import com.tchalanet.server.core.promotion.internal.application.port.out.lifecycle.PromotionCampaignWritePort;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionCampaignTransition;
import com.tchalanet.server.core.promotion.internal.domain.service.PromotionCampaignStateMachine;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class PausePromotionCampaignCommandHandler implements CommandHandler<PausePromotionCampaignCommand, PromotionCampaignView> {

    private final PromotionCampaignReadPort readerPort;
    private final PromotionCampaignWritePort writePort;
    private final PromotionCampaignStateMachine stateMachine;
    private final PromotionCacheEvictorPort cacheEvictor;

    @Override
    @TchTx
    public PromotionCampaignView handle(PausePromotionCampaignCommand cmd) {
        var campaign = readerPort.getRequired(cmd.campaignId());

        var nextStatus = stateMachine.apply(campaign.status(), PromotionCampaignTransition.PAUSE);

        var out = writePort.changeStatus(cmd.tenantId(), cmd.campaignId(), nextStatus);
        AfterCommit.run(() -> cacheEvictor.evictAfterCampaignMutation(cmd.tenantId(), cmd.campaignId()));
        return out;
    }
}


