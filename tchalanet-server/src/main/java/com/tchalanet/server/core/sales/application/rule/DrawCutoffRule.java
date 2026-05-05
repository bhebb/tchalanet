package com.tchalanet.server.core.sales.application.rule;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.query.model.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
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
        var draw = queryBus.send(new GetDrawByIdQuery(drawId));
        var now = Instant.now(clock);
        var cutoff = draw.cutoffAt();

        if (now.isAfter(cutoff)) {
            throw ProblemRest.conflict("Draw cutoff time has passed");
        }
        return draw;
    }
}

