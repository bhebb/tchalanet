package com.tchalanet.server.core.promotion.internal.application.command.handler.template;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.TchTimeProvider;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.promotion.api.command.template.InstantiateDefaultMaryajGratisCommand;
import com.tchalanet.server.core.promotion.api.model.lifecycle.PromotionCampaignView;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionCacheEvictorPort;
import com.tchalanet.server.core.promotion.internal.application.port.out.lifecycle.PromotionCampaignReadPort;
import com.tchalanet.server.core.promotion.internal.application.port.out.lifecycle.PromotionCampaignWritePort;
import com.tchalanet.server.core.promotion.internal.application.service.lifecycle.PromotionCampaignActivationPolicy;
import com.tchalanet.server.core.promotion.internal.application.service.template.MaryajGratisDefaultTemplate;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionCampaignTransition;
import com.tchalanet.server.core.promotion.internal.domain.service.PromotionCampaignStateMachine;
import lombok.RequiredArgsConstructor;

/**
 * Template plateforme -> instance tenant : crée la campagne DEFAULT_MARYAJ_GRATIS
 * pour le tenant courant et l'active. Idempotent par code de campagne.
 */
@UseCase
@RequiredArgsConstructor
public class InstantiateDefaultMaryajGratisCommandHandler
    implements CommandHandler<InstantiateDefaultMaryajGratisCommand, PromotionCampaignView> {

    private final PromotionCampaignReadPort readPort;
    private final PromotionCampaignWritePort writePort;
    private final PromotionCampaignStateMachine stateMachine;
    private final PromotionCampaignActivationPolicy activationPolicy;
    private final PromotionCacheEvictorPort cacheEvictor;
    private final TchTimeProvider timeProvider;

    @Override
    @TchTx
    public PromotionCampaignView handle(InstantiateDefaultMaryajGratisCommand cmd) {
        var existing = readPort.findByCode(MaryajGratisDefaultTemplate.CODE);
        if (existing.isPresent()) {
            return existing.get();
        }

        var created = writePort.create(
            MaryajGratisDefaultTemplate.createCommand(cmd.tenantId(), timeProvider.now()));

        activationPolicy.validate(created);
        var nextStatus = stateMachine.apply(created.status(), PromotionCampaignTransition.ACTIVATE);
        var activated = writePort.changeStatus(cmd.tenantId(), created.id(), nextStatus);

        AfterCommit.run(() ->
            cacheEvictor.evictAfterCampaignMutation(cmd.tenantId(), created.id()));

        return activated;
    }
}
