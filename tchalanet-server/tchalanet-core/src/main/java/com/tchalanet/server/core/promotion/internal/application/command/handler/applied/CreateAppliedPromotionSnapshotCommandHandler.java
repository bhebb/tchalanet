package com.tchalanet.server.core.promotion.internal.application.command.handler.applied;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.promotion.api.command.applied.CreateAppliedPromotionSnapshotCommand;
import com.tchalanet.server.core.promotion.api.model.AppliedPromotionSnapshotResult;
import com.tchalanet.server.core.promotion.api.model.PromotionDecisionStatus;
import com.tchalanet.server.core.promotion.internal.application.port.out.AppliedPromotionSnapshotPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateAppliedPromotionSnapshotCommandHandler
    implements CommandHandler<CreateAppliedPromotionSnapshotCommand, AppliedPromotionSnapshotResult> {

    private final AppliedPromotionSnapshotPort port;

    @Override
    @TchTx
    public AppliedPromotionSnapshotResult handle(CreateAppliedPromotionSnapshotCommand c) {
        if (c.decision().status() != PromotionDecisionStatus.APPLIED) {
            throw ProblemRest.badRequest("promotionDecision.decision_not_applied");
        }
        return port.createIfAbsent(c.ticketId(), c.decision());
    }
}

