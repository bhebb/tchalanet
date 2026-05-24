package com.tchalanet.server.core.promotion.internal.application.command.handler.rule;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.promotion.api.command.rule.AddPromotionRuleCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignView;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCampaignWritePort;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCacheEvictorPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AddPromotionRuleCommandHandler implements CommandHandler<AddPromotionRuleCommand, PromotionCampaignView> {

    private final PromotionCampaignWritePort writePort;
    private final PromotionCacheEvictorPort cacheEvictor;

    @Override
    public PromotionCampaignView handle(AddPromotionRuleCommand cmd) {
        var out = writePort.addRule(cmd);
        AfterCommit.run(() -> cacheEvictor.evictAfterCampaignMutation(cmd.tenantId(), cmd.campaignId()));
        return out;
    }
}


