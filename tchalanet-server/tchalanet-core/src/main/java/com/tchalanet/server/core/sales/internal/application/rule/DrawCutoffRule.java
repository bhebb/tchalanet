package com.tchalanet.server.core.sales.internal.application.rule;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.internal.domain.model.DrawStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DrawCutoffRule {

    private final QueryBus queryBus;
    private final Clock clock;

    /**
     * Returns the resolved draw (reuse in handler) and ensures sale is still allowed.
     */
    public DrawSummary requireBeforeCutoff(DrawId drawId) {
        var draw = queryBus.ask(new GetDrawByIdQuery(drawId));
        var now = Instant.now(clock);
        var cutoff = draw.cutoffAt();

        if (draw.status() != DrawStatus.OPEN) {
            throw ProblemRest.conflict("Draw is not open for sales");
        }

        if (!now.isBefore(cutoff)) {
            throw ProblemRest.conflict("Draw cutoff time has passed");
        }
        return draw;
    }
}
