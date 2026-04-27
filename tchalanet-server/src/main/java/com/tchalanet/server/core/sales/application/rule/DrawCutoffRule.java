package com.tchalanet.server.core.sales.application.rule;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.query.model.GetDrawQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
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
    public Draw requireBeforeCutoff(DrawId drawId) {
        Draw draw = queryBus.send(new GetDrawQuery(drawId));
        Instant now = Instant.now(clock);
        Instant cutoff = draw.cutoffAt().toInstant();

        if (now.isAfter(cutoff)) {
            throw ProblemRest.conflict("Draw cutoff time has passed");
        }
        return draw;
    }
}

